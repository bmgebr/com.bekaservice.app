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
    val deliveryMethodCode: String = "pickup",
    val deliveryCity: String = "ახალციხე",
    val deliveryFeeText: String = "უფასო (0 ₾)",
    val deviceModel: String = "",
    val serviceType: String = "",
    val issueDescription: String = "",
    val items: List<Map<String, Any>> = emptyList(),
    val status: String = "pending",
    val price: String = "",
    val totalAmount: Double = 0.0,
    val currency: String = "GEL",
    val paymentMethod: String = "ნაღდი ანგარიშსწორება",
    val paymentMethodCode: String = "",
    val paymentStatus: String = "",
    val bankAccountId: String = "",
    val bankCode: String = "",
    val bankName: String = "",
    val recipientNameSnapshot: String = "",
    val ibanSnapshot: String = "",
    val paymentReference: String = "",
    val paymentInstructionsSnapshot: String = "",
    val userConfirmedPaymentAtMillis: Long = 0L,
    val paymentConfirmedAtMillis: Long = 0L,
    val paymentConfirmedBy: String = "",
    val paymentInternalNote: String = "",
    val acceptedTermsVersion: String = "",
    val acceptedTermsAtMillis: Long = 0L,
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
        val resolvedOrdNumber = orderNumber.ifBlank { "#BS-${resolvedId.takeLast(4).uppercase()}" }
        val resolvedMethodType = PaymentMethodType.fromCode(paymentMethodCode, paymentMethod)
        val resolvedPayStatus = PaymentStatus.fromCode(paymentStatus, resolvedMethodType)

        return OrderRecord(
            id = resolvedId,
            orderNumber = resolvedOrdNumber,
            date = formattedDate,
            items = parsedItems,
            totalAmount = resolvedPrice,
            status = uiStatus,
            paymentMethod = paymentMethod.ifBlank { resolvedMethodType.titleKa },
            trackingCode = trackingCode.ifBlank { "BS-TRK-${resolvedId.takeLast(5).uppercase()}" },
            userId = userId,
            userName = userName,
            userPhone = userPhone,
            userEmail = userEmail,
            deliveryAddress = deliveryAddress,
            deliveryMethodCode = deliveryMethodCode.ifBlank { "pickup" },
            deliveryCity = deliveryCity.ifBlank { "ახალციხე" },
            deliveryFeeText = deliveryFeeText.ifBlank { "უფასო (0 ₾)" },
            paymentMethodCode = resolvedMethodType.code,
            paymentStatus = resolvedPayStatus.code,
            bankAccountId = bankAccountId,
            bankCode = bankCode,
            bankName = bankName,
            recipientNameSnapshot = recipientNameSnapshot,
            ibanSnapshot = ibanSnapshot,
            paymentReference = paymentReference.ifBlank { resolvedOrdNumber },
            paymentInstructionsSnapshot = paymentInstructionsSnapshot,
            userConfirmedPaymentAtMillis = userConfirmedPaymentAtMillis,
            paymentConfirmedAtMillis = paymentConfirmedAtMillis,
            paymentConfirmedBy = paymentConfirmedBy,
            paymentInternalNote = paymentInternalNote,
            acceptedTermsVersion = acceptedTermsVersion,
            acceptedTermsAtMillis = acceptedTermsAtMillis,
            fulfillmentStatusCode = status,
            createdAtMillis = createdAt?.toDate()?.time ?: System.currentTimeMillis()
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
    val orderType: String = "service_order", // "service_order"
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
    // ADMIN ROLE VERIFICATION & CLAIM SYNCHRONIZATION
    // (Server-Side Token Claims, syncSoleAdminCustomClaims & /admins/{uid} Collection)
    // ==========================================================================
    private fun invokeSyncSoleAdminCallable(projectId: String, idToken: String): Boolean {
        val endpoint = "https://us-central1-$projectId.cloudfunctions.net/syncSoleAdminCustomClaims"
        val connection = (java.net.URL(endpoint).openConnection() as java.net.HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 2500
            readTimeout = 2500
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Authorization", "Bearer $idToken")
        }
        return try {
            connection.outputStream.use { os ->
                os.write("""{"data":{}}""".toByteArray(Charsets.UTF_8))
            }
            val code = connection.responseCode
            if (code in 200..299) {
                val body = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(body)
                val resultObj = json.optJSONObject("result") ?: json.optJSONObject("data")
                resultObj?.optBoolean("authorized", false) == true &&
                    resultObj.optBoolean("customClaimAdmin", false)
            } else {
                false
            }
        } finally {
            connection.disconnect()
        }
    }

    /**
     * Synchronizes and verifies the sole administrator's (`beqa1994@gmail.com`) server-side
     * authorization and Firebase Custom Claim (`admin: true`):
     * 1. Verifies the currently authenticated Firebase user's email and email-verified status.
     * 2. Calls the `syncSoleAdminCustomClaims` Cloud Function using the caller's verified ID token
     *    (never passing a client-supplied UID or email).
     * 3. Force-refreshes the Firebase Auth ID token (`getIdToken(true)`) so updated custom claims
     *    take effect immediately without signing out or reinstalling the app.
     * 4. Ensures `/admins/{uid}` is synchronized and verified against Firestore Security Rules.
     */
    suspend fun syncSoleAdminCustomClaims(): Result<Boolean> {
        val firebaseAuth = runCatching { auth }.getOrNull()
        val currentUser = firebaseAuth?.currentUser
            ?: return Result.failure(IllegalStateException("Authentication is required."))

        return try {
            val initialToken = runCatching { currentUser.getIdToken(false).await() }.getOrNull()
            val initialClaims = initialToken?.claims.orEmpty()

            val email = (initialClaims["email"] as? String)
                ?.takeIf { it.isNotBlank() }
                ?: currentUser.email?.takeIf { it.isNotBlank() }
                ?: currentUser.providerData.firstNotNullOfOrNull { it.email?.takeIf { e -> e.isNotBlank() } }
                ?: ""

            if (!AuthAndOrderManager.isSoleAdminEmail(email)) {
                return Result.failure(
                    SecurityException("Only the sole authorized administrator (beqa1994@gmail.com) may obtain admin claims.")
                )
            }

            val signInProvider = initialToken?.signInProvider
                ?: ((initialClaims["firebase"] as? Map<*, *>)?.get("sign_in_provider") as? String)
                ?: ""
            val isEmailVerified = (initialClaims["email_verified"] as? Boolean) != false ||
                currentUser.isEmailVerified ||
                signInProvider == "password"
            if (!isEmailVerified) {
                return Result.failure(
                    SecurityException("Administrator email address is not verified.")
                )
            }

            // 1. Check if /admins/{uid} already exists and whether it is explicitly disabled
            val adminDocRef = db.collection("admins").document(currentUser.uid)
            val existingAdminDoc = runCatching { adminDocRef.get().await() }.getOrNull()
            if (existingAdminDoc != null && existingAdminDoc.exists()) {
                if (existingAdminDoc.getBoolean("enabled") != true) {
                    return Result.success(false)
                }
            }

            // 2. Invoke server-side syncSoleAdminCustomClaims Cloud Function when targeting live project
            val projectId = runCatching { db.app.options.projectId }.getOrNull().orEmpty()
            val idTokenStr = initialToken?.token.orEmpty()
            if (idTokenStr.isNotBlank() && projectId.isNotBlank() && projectId != "demo-no-project") {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    runCatching {
                        invokeSyncSoleAdminCallable(projectId, idTokenStr)
                    }.onFailure { e ->
                        Log.d("FirestoreRepo", "Callable syncSoleAdminCustomClaims fallback: ${e.message}")
                    }
                }
            }

            // 3. Force-refresh the Firebase ID token so any newly assigned custom claim is loaded immediately
            val refreshedToken = runCatching { currentUser.getIdToken(true).await() }.getOrNull() ?: initialToken
            val refreshedClaims = refreshedToken?.claims.orEmpty()
            val hasCustomClaim = refreshedClaims["admin"] == true

            // 4. Ensure /admins/{uid} is synchronized on the Firestore server (enforced by isValidSoleAdminSelfSync rule)
            if (existingAdminDoc == null || !existingAdminDoc.exists()) {
                val syncPayload = mapOf(
                    "uid" to currentUser.uid,
                    "email" to AuthAndOrderManager.SOLE_ADMIN_EMAIL,
                    "enabled" to true,
                    "role" to "sole_admin",
                    "customClaimAdmin" to true,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                adminDocRef.set(syncPayload, SetOptions.merge()).await()
            }

            val verifiedDoc = runCatching { adminDocRef.get().await() }.getOrNull()
            val isServerDocActive = verifiedDoc != null &&
                verifiedDoc.exists() &&
                verifiedDoc.getBoolean("enabled") == true &&
                (verifiedDoc.getString("email").orEmpty().let { it.isBlank() || AuthAndOrderManager.isSoleAdminEmail(it) })

            val authorized = hasCustomClaim || isServerDocActive
            if (authorized && appContext != null) {
                AuthAndOrderManager.setAdminClaimVerified(appContext, true)
            }
            Result.success(authorized)
        } catch (e: Exception) {
            Log.w("FirestoreRepo", "syncSoleAdminCustomClaims failed: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun checkIsCurrentUserAdmin(): Boolean {
        val firebaseAuth = runCatching { auth }.getOrNull()
        val currentUser = firebaseAuth?.currentUser
        if (currentUser == null) {
            return appContext != null && AuthAndOrderManager.hasVerifiedAdminClaim(appContext)
        }
        return try {
            val tokenResult = runCatching { currentUser.getIdToken(false).await() }.getOrNull()
            val claims = tokenResult?.claims.orEmpty()

            val email = (claims["email"] as? String)
                ?.takeIf { it.isNotBlank() }
                ?: currentUser.email?.takeIf { it.isNotBlank() }
                ?: currentUser.providerData.firstNotNullOfOrNull { it.email?.takeIf { e -> e.isNotBlank() } }
                ?: ""

            // Strictly enforce single authorized administrator email ("beqa1994@gmail.com")
            if (!AuthAndOrderManager.isSoleAdminEmail(email)) {
                return false
            }

            val signInProvider = tokenResult?.signInProvider
                ?: ((claims["firebase"] as? Map<*, *>)?.get("sign_in_provider") as? String)
                ?: ""
            val isEmailVerified = (claims["email_verified"] as? Boolean) != false ||
                currentUser.isEmailVerified ||
                signInProvider == "password"
            if (!isEmailVerified) {
                return false
            }

            // 1. Check server-side /admins/{uid} authorization document first (respects explicit revocation if enabled == false)
            val adminDoc = runCatching {
                db.collection("admins").document(currentUser.uid).get().await()
            }.getOrNull()
            if (adminDoc != null && adminDoc.exists()) {
                if (adminDoc.getBoolean("enabled") != true) {
                    return false
                }
                val docEmail = adminDoc.getString("email").orEmpty()
                if (docEmail.isBlank() || AuthAndOrderManager.isSoleAdminEmail(docEmail)) {
                    return true
                }
                return false
            }

            // 2. Verify Firebase Custom Claim ("admin": true) on the ID token
            if (claims["admin"] == true) {
                return true
            }

            // 3. Synchronize sole admin custom claims & server-side /admins/{uid} record and force-refresh token
            val synced = syncSoleAdminCustomClaims().getOrDefault(false)
            if (synced) {
                return true
            }

            // Do not bypass the custom claim check based solely on client-side email comparison
            Log.w(
                "FirestoreRepo",
                "Authenticated user ($email) does not have 'admin: true' custom claim or active /admins/${currentUser.uid} document."
            )
            false
        } catch (e: Exception) {
            Log.w("FirestoreRepo", "Admin check failed: ${e.message}")
            false
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

    /**
     * Google Play Policy Compliance: Completely deletes the user's Firestore document,
     * device tokens, and Firebase Auth account.
     */
    suspend fun deleteCurrentUserAccount(): Result<Unit> {
        val currentUser = auth.currentUser
        return try {
            if (currentUser != null) {
                val uid = currentUser.uid
                runCatching {
                    db.collection("users").document(uid).delete().await()
                }
                runCatching {
                    val tokens = db.collection("fcm_tokens").whereEqualTo("userId", uid).get().await()
                    for (doc in tokens.documents) {
                        doc.reference.delete().await()
                    }
                }
                currentUser.delete().await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirestoreRepo", "Failed to delete user account: ${e.message}", e)
            Result.failure(e)
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
                createAdminOrderNotification(
                    orderId = orderId,
                    orderType = "service_order",
                    createdByUid = uid,
                    orderNumber = "#BS-${orderId.takeLast(4)}",
                    summaryTitle = serviceType,
                    priceText = price
                )
            }
            appContext?.let { ctx ->
                BekaNotificationManager.recordOrderCreatedForAdminOnly(
                    context = ctx,
                    orderId = orderId,
                    orderType = "service_order",
                    orderNumber = "#BS-${orderId.takeLast(4)}",
                    summaryText = serviceType,
                    amountText = price
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
            "userName" to (orderRecord.userName.ifBlank { user?.displayName ?: currentUser.displayName ?: "მომხმარებელი" }).take(100),
            "userPhone" to (orderRecord.userPhone.ifBlank { user?.phone ?: "+995 598 23 33 32" }).ifBlank { "+995 598 23 33 32" }.take(30),
            "userEmail" to (orderRecord.userEmail.ifBlank { user?.email ?: currentUser.email ?: "" }).take(150),
            "deliveryAddress" to (orderRecord.deliveryAddress.ifBlank { user?.address ?: "ქ. ახალციხე, კოსტავას N10" }).take(300),
            "deliveryMethodCode" to orderRecord.deliveryMethodCode.ifBlank { "pickup" }.take(30),
            "deliveryCity" to orderRecord.deliveryCity.ifBlank { "ახალციხე" }.take(100),
            "deliveryFeeText" to orderRecord.deliveryFeeText.ifBlank { "უფასო (0 ₾)" }.take(120),
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
            "paymentMethodCode" to orderRecord.paymentMethodCode.ifBlank { "cod" }.take(40),
            "paymentStatus" to (if (orderRecord.paymentMethodCode == "bank_transfer") "pending" else "awaiting_collection"),
            "bankAccountId" to orderRecord.bankAccountId.take(80),
            "bankCode" to orderRecord.bankCode.take(40),
            "bankName" to orderRecord.bankName.take(120),
            "recipientNameSnapshot" to orderRecord.recipientNameSnapshot.take(160),
            "ibanSnapshot" to orderRecord.ibanSnapshot.take(40),
            "paymentReference" to orderRecord.paymentReference.ifBlank { orderRecord.orderNumber }.take(60),
            "paymentInstructionsSnapshot" to orderRecord.paymentInstructionsSnapshot.take(1000),
            "userConfirmedPaymentAtMillis" to 0L,
            "acceptedTermsVersion" to orderRecord.acceptedTermsVersion.ifBlank { BstorePaymentManager.CURRENT_TERMS_VERSION }.take(60),
            "acceptedTermsAtMillis" to (if (orderRecord.acceptedTermsAtMillis > 0L) orderRecord.acceptedTermsAtMillis else System.currentTimeMillis()),
            "trackingCode" to orderRecord.trackingCode.take(60),
            "adminRead" to false,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        return try {
            val createdNew = db.runTransaction { tx ->
                val existingSnap = tx.get(docRef)
                if (!existingSnap.exists()) {
                    tx.set(docRef, payload)
                    true
                } else {
                    false
                }
            }.await()
            Log.d("FirestoreRepo", "Cart order saved idempotently (createdNew=$createdNew): $safeOrderId")
            if (createdNew) {
                runCatching {
                    createAdminOrderNotification(
                        orderId = safeOrderId,
                        orderType = "service_order",
                        createdByUid = uid,
                        orderNumber = orderRecord.orderNumber,
                        summaryTitle = summaryTitle,
                        priceText = orderRecord.totalAmount
                    )
                }
                appContext?.let { ctx ->
                    BekaNotificationManager.recordOrderCreatedForAdminOnly(
                        context = ctx,
                        orderId = safeOrderId,
                        orderType = "service_order",
                        orderNumber = orderRecord.orderNumber,
                        summaryText = summaryTitle,
                        amountText = orderRecord.totalAmount
                    )
                }
            }
            Result.success(safeOrderId)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    /**
     * Saves a customer's product availability inquiry from the AI Assistant directly to `/orders`
     * as a potential order / product request (`#REQ-...`) strictly adhering to `isValidServiceOrder`
     * rules, and dispatches a sanitized `/admin_notifications` alert to the sole administrator.
     */
    suspend fun createProductStockInquiryOrder(
        orderRecord: OrderRecord,
        requestedProduct: String,
        customerNote: String = ""
    ): Result<String> {
        val currentUser = auth.currentUser
            ?: return Result.failure(IllegalStateException("მარაგის მოთხოვნის ონლაინ გასაგზავნად საჭიროა Google ავტორიზაცია"))

        val uid = currentUser.uid
        val safeOrderId = orderRecord.id.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
            .ifBlank { "req_${System.currentTimeMillis()}" }
        val cleanProduct = requestedProduct.trim().ifBlank { "სასურველი პროდუქტი" }.take(200)
        val cleanNote = customerNote.trim().take(1500)
        val docRef = db.collection("orders").document(safeOrderId)

        val snapshotItems = orderRecord.items.take(50).map { item ->
            hashMapOf<String, Any>(
                "productTitle" to item.productTitle.take(200),
                "name" to item.productTitle.take(200),
                "quality" to item.quality.take(100),
                "quantity" to item.quantity.coerceAtLeast(1),
                "price" to item.price.take(50),
                "imageUrl" to item.imageUrl.take(1000)
            )
        }

        val summaryServiceType = "პროდუქტის მოთხოვნა / პოტენციური შეკვეთა: $cleanProduct".take(300)
        val detailsDesc = buildString {
            append("მოთხოვნილი პროდუქტი (მარაგის დაზუსტება): $cleanProduct\n")
            if (cleanNote.isNotBlank()) {
                append("კომენტარი: $cleanNote\n")
            }
            append("საკონტაქტო: ${orderRecord.userPhone}")
        }.take(4000)
        val acceptedMillis = if (orderRecord.acceptedTermsAtMillis > 0L) {
            orderRecord.acceptedTermsAtMillis
        } else {
            System.currentTimeMillis()
        }

        val payload = hashMapOf<String, Any>(
            "id" to safeOrderId,
            "orderNumber" to orderRecord.orderNumber.take(40),
            "userId" to uid,
            "userName" to (orderRecord.userName.ifBlank { currentUser.displayName ?: "მომხმარებელი" }).take(100),
            "userPhone" to orderRecord.userPhone.ifBlank { "+995 598 23 33 32" }.take(30),
            "userEmail" to (orderRecord.userEmail.ifBlank { currentUser.email ?: "" }).take(150),
            "deliveryAddress" to orderRecord.deliveryAddress.ifBlank { "დაზუსტდება ზარით / WhatsApp-ით" }.take(300),
            "deliveryMethodCode" to "pickup",
            "deliveryCity" to orderRecord.deliveryCity.ifBlank { "ახალციხე" }.take(100),
            "deliveryFeeText" to orderRecord.deliveryFeeText.ifBlank { "უფასო (0 ₾)" }.take(120),
            "deviceModel" to cleanProduct.take(100),
            "serviceType" to summaryServiceType,
            "issueDescription" to detailsDesc,
            "items" to snapshotItems,
            "status" to "pending",
            "price" to "ფასი შეთანხმებით",
            "totalAmount" to 0.0,
            "priceVerified" to false,
            "currency" to "GEL",
            "paymentMethod" to orderRecord.paymentMethod.take(120),
            "paymentMethodCode" to "legacy_unspecified",
            "paymentStatus" to "pending",
            "paymentReference" to orderRecord.orderNumber.take(60),
            "paymentInternalNote" to cleanNote.take(500),
            "userConfirmedPaymentAtMillis" to 0L,
            "acceptedTermsVersion" to "stock_inquiry_v1",
            "acceptedTermsAtMillis" to acceptedMillis,
            "trackingCode" to orderRecord.trackingCode.take(60),
            "adminRead" to false,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        return try {
            docRef.set(payload).await()
            Log.d("FirestoreRepo", "Product stock inquiry order saved to Firestore: $safeOrderId")
            runCatching {
                createAdminOrderNotification(
                    orderId = safeOrderId,
                    orderType = "service_order",
                    createdByUid = uid,
                    orderNumber = orderRecord.orderNumber,
                    summaryTitle = cleanProduct,
                    priceText = "მარაგის დაზუსტება",
                    isStockInquiry = true
                )
            }
            appContext?.let { ctx ->
                BekaNotificationManager.recordProductInquiryForAdminOnly(
                    context = ctx,
                    orderId = safeOrderId,
                    orderNumber = orderRecord.orderNumber,
                    requestedProduct = cleanProduct,
                    customerNote = cleanNote
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

    suspend fun switchOrderBankByCustomer(
        order: OrderRecord,
        bank: BankAccountConfig
    ): Result<OrderRecord> {
        val currentUser = auth.currentUser
            ?: return Result.failure(IllegalStateException("საბანკო ბანკის შესაცვლელად საჭიროა ავტორიზაცია"))
        if (order.paymentMethodCode != PaymentMethodType.BANK_TRANSFER.code) {
            return Result.failure(IllegalArgumentException("ბანკის შეცვლა შესაძლებელია მხოლოდ საბანკო გადარიცხვის შეკვეთაზე"))
        }
        val currentPayStatus = PaymentStatus.fromCode(order.paymentStatus, PaymentMethodType.BANK_TRANSFER)
        if (currentPayStatus != PaymentStatus.PENDING) {
            return Result.failure(IllegalStateException("ბანკის შეცვლა შესაძლებელია მხოლოდ გადახდის მოლოდინში მყოფ შეკვეთაზე"))
        }
        if (!bank.isEnabled || !BstorePaymentManager.isValidIban(bank.iban) || bank.recipientName.isBlank()) {
            return Result.failure(IllegalArgumentException("არჩეული საბანკო ანგარიში მიუწვდომელია"))
        }

        val methodLabel = "საბანკო გადარიცხვა (${bank.bankName})"
        val instructions = bank.paymentInstructions.ifBlank { BankAccountConfig.DEFAULT_PAYMENT_INSTRUCTIONS_KA }
        val docRef = db.collection("orders").document(order.id)

        return try {
            if (order.userId.isBlank() || order.userId == currentUser.uid) {
                docRef.update(
                    mapOf(
                        "paymentStatus" to PaymentStatus.PENDING.code,
                        "bankAccountId" to bank.id.take(80),
                        "bankCode" to bank.bankCode.take(40),
                        "bankName" to bank.bankName.take(120),
                        "recipientNameSnapshot" to bank.recipientName.take(160),
                        "ibanSnapshot" to BstorePaymentManager.normalizeIban(bank.iban).take(40),
                        "paymentMethod" to methodLabel.take(120),
                        "paymentInstructionsSnapshot" to instructions.take(1000),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                return Result.failure(SecurityException("PERMISSION_DENIED: სხვა მომხმარებლის შეკვეთის შეცვლა აკრძალულია"))
            }
            val updated = appContext?.let { ctx ->
                AuthAndOrderManager.switchOrderBankLocal(ctx, order.id, bank)
            } ?: order.copy(
                bankAccountId = bank.id,
                bankCode = bank.bankCode,
                bankName = bank.bankName,
                recipientNameSnapshot = bank.recipientName,
                ibanSnapshot = BstorePaymentManager.normalizeIban(bank.iban),
                paymentMethod = methodLabel,
                paymentInstructionsSnapshot = instructions
            )
            Result.success(updated)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun confirmTransferSentByCustomer(
        order: OrderRecord,
        selectedBank: BankAccountConfig? = null
    ): Result<OrderRecord> {
        val currentUser = auth.currentUser
            ?: return Result.failure(IllegalStateException("გადარიცხვის დასადასტურებლად საჭიროა ავტორიზაცია"))
        if (order.paymentMethodCode != PaymentMethodType.BANK_TRANSFER.code) {
            return Result.failure(IllegalArgumentException("ეს მოქმედება განკუთვნილია მხოლოდ საბანკო გადარიცხვისთვის"))
        }
        val currentPayStatus = PaymentStatus.fromCode(order.paymentStatus, PaymentMethodType.BANK_TRANSFER)
        if (currentPayStatus == PaymentStatus.VERIFICATION_PENDING || currentPayStatus == PaymentStatus.PAID) {
            return Result.success(order)
        }
        if (currentPayStatus != PaymentStatus.PENDING || order.status == "გაუქმებული") {
            return Result.failure(IllegalStateException("მიმდინარე სტატუსზე გადარიცხვის დადასტურება შეუძლებელია"))
        }
        if (order.userId.isNotBlank() && order.userId != currentUser.uid) {
            return Result.failure(SecurityException("PERMISSION_DENIED: სხვა მომხმარებლის შეკვეთაზე წვდომა აკრძალულია"))
        }

        val nowMillis = System.currentTimeMillis()
        val docRef = db.collection("orders").document(order.id)
        val updates = hashMapOf<String, Any>(
            "paymentStatus" to PaymentStatus.VERIFICATION_PENDING.code,
            "userConfirmedPaymentAtMillis" to nowMillis,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (selectedBank != null && selectedBank.isEnabled && BstorePaymentManager.isValidIban(selectedBank.iban)) {
            updates["bankAccountId"] = selectedBank.id.take(80)
            updates["bankCode"] = selectedBank.bankCode.take(40)
            updates["bankName"] = selectedBank.bankName.take(120)
            updates["recipientNameSnapshot"] = selectedBank.recipientName.take(160)
            updates["ibanSnapshot"] = BstorePaymentManager.normalizeIban(selectedBank.iban).take(40)
            updates["paymentMethod"] = "საბანკო გადარიცხვა (${selectedBank.bankName})".take(120)
            updates["paymentInstructionsSnapshot"] = selectedBank.paymentInstructions
                .ifBlank { BankAccountConfig.DEFAULT_PAYMENT_INSTRUCTIONS_KA }
                .take(1000)
        }

        return try {
            docRef.update(updates).await()

            val ordNum = order.orderNumber.ifBlank { "#BS-${order.id.takeLast(4).uppercase()}" }
            val safeTitle = BekaNotificationManager.stripSensitiveDataFromPayload(
                "გადარიცხვა დადასტურდა $ordNum"
            ).take(120)
            val safeBody = BekaNotificationManager.stripSensitiveDataFromPayload(
                "მომხმარებელმა დაადასტურა $ordNum შეკვეთის თანხის გადმორიცხვა. საჭიროა საბანკო ანგარიშზე შემოწმება."
            ).take(400)
            val notifId = "admin_order_${order.id}"
            val adminNotifRef = db.collection("admin_notifications").document(notifId)
            val updateResult = runCatching {
                adminNotifRef.update(
                    mapOf(
                        "title" to safeTitle,
                        "body" to safeBody,
                        "actorUid" to currentUser.uid,
                        "read" to false,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            if (updateResult.isFailure) {
                runCatching {
                    adminNotifRef.set(
                        hashMapOf<String, Any>(
                            "id" to notifId,
                            "notificationId" to notifId,
                            "orderId" to order.id,
                            "orderType" to "service_order",
                            "title" to safeTitle,
                            "body" to safeBody,
                            "deepLink" to "bekaservice://admin/orders/service/${order.id}",
                            "actorUid" to currentUser.uid,
                            "targetAdminEmail" to AuthAndOrderManager.SOLE_ADMIN_EMAIL,
                            "read" to false,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            }

            val updatedOrder = appContext?.let { ctx ->
                AuthAndOrderManager.markOrderTransferSentByUserLocal(ctx, order.id, selectedBank).getOrNull()
            } ?: order.copy(
                paymentStatus = PaymentStatus.VERIFICATION_PENDING.code,
                userConfirmedPaymentAtMillis = nowMillis,
                bankAccountId = selectedBank?.id ?: order.bankAccountId,
                bankCode = selectedBank?.bankCode ?: order.bankCode,
                bankName = selectedBank?.bankName ?: order.bankName,
                recipientNameSnapshot = selectedBank?.recipientName ?: order.recipientNameSnapshot,
                ibanSnapshot = selectedBank?.let { BstorePaymentManager.normalizeIban(it.iban) } ?: order.ibanSnapshot,
                paymentMethod = selectedBank?.let { "საბანკო გადარიცხვა (${it.bankName})" } ?: order.paymentMethod
            )
            Result.success(updatedOrder)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun confirmOrderPaymentAsAdmin(
        order: OrderRecord,
        internalNote: String = ""
    ): Result<OrderRecord> {
        if (order.id.isBlank()) return Result.failure(IllegalArgumentException("ცარიელი შეკვეთის ID"))
        if (!checkIsCurrentUserAdmin() || auth.currentUser == null) {
            return Result.failure(
                IllegalStateException("PERMISSION_DENIED: გადახდის დადასტურებისთვის საჭიროა ვერიფიცირებული ადმინისტრატორის ავტორიზაცია (beqa1994@gmail.com)")
            )
        }
        val currentPayStatus = PaymentStatus.fromCode(
            order.paymentStatus,
            PaymentMethodType.fromCode(order.paymentMethodCode, order.paymentMethod)
        )
        if (currentPayStatus == PaymentStatus.PAID) {
            return Result.success(order)
        }
        val adminUid = auth.currentUser?.uid ?: "sole_admin"
        val nowMillis = System.currentTimeMillis()
        val methodType = PaymentMethodType.fromCode(order.paymentMethodCode, order.paymentMethod)
        val prevPaymentStatus = order.paymentStatus
        val docRef = db.collection("orders").document(order.id)

        return try {
            docRef.update(
                mapOf(
                    "paymentStatus" to PaymentStatus.PAID.code,
                    "paymentConfirmedAtMillis" to nowMillis,
                    "paymentConfirmedBy" to adminUid,
                    "paymentInternalNote" to internalNote.trim().take(500),
                    "priceVerified" to true,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            val numericAmount = order.totalAmount.filter { it.isDigit() || it == '.' }.toDoubleOrNull() ?: 0.0
            val eventType = if (methodType == PaymentMethodType.COD) {
                "COD_COLLECTION_CONFIRMED"
            } else {
                "BANK_TRANSFER_CONFIRMED"
            }
            val auditId = "audit_${nowMillis}_${order.id.takeLast(6)}"
            val auditPayload = hashMapOf<String, Any>(
                "id" to auditId,
                "orderId" to order.id,
                "orderNumber" to order.orderNumber,
                "eventType" to eventType,
                "previousStatus" to prevPaymentStatus,
                "newStatus" to PaymentStatus.PAID.code,
                "amount" to numericAmount,
                "currency" to "GEL",
                "actorUid" to adminUid,
                "actorEmail" to AuthAndOrderManager.SOLE_ADMIN_EMAIL,
                "actorRole" to "sole_admin",
                "timestampMillis" to nowMillis,
                "timestamp" to nowMillis,
                "note" to internalNote.trim().take(500),
                "createdAt" to FieldValue.serverTimestamp()
            )
            runCatching {
                db.collection("payment_audit_logs").document(auditId).set(auditPayload).await()
            }

            // Send targeted PRIVATE_USER customer notification (NEVER fall back to "all" if userId is missing!)
            val ordNum = order.orderNumber.ifBlank { "#BS-${order.id.takeLast(4).uppercase()}" }
            val cleanRecipientUid = order.userId.trim()
            if (cleanRecipientUid.isNotBlank()) {
                runCatching {
                    sendAdminNotification(
                        title = "გადახდა დადასტურდა $ordNum",
                        body = "თქვენი $ordNum შეკვეთის გადახდა დადასტურდა.",
                        targetTab = 4,
                        type = "order_update",
                        explicitNotificationId = "notif_order_paid_${order.id}",
                        orderId = order.id,
                        orderNumber = ordNum,
                        audience = "user:$cleanRecipientUid",
                        visibility = "PRIVATE_USER",
                        recipientUid = cleanRecipientUid,
                        relatedEntityId = order.id
                    )
                }
            } else {
                Log.w("BekaPush", "Skipping customer payment notification for ${order.id}: missing order.userId")
            }

            val updatedRecord = order.copy(
                paymentStatus = PaymentStatus.PAID.code,
                paymentConfirmedAtMillis = nowMillis,
                paymentConfirmedBy = adminUid,
                paymentInternalNote = internalNote.trim()
            )
            appContext?.let { ctx ->
                AuthAndOrderManager.confirmOrderPaymentLocalAsAdmin(ctx, order, internalNote)
            }
            Result.success(updatedRecord)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateOrderPaymentStatusAsAdmin(
        order: OrderRecord,
        newPaymentStatus: PaymentStatus,
        internalNote: String = ""
    ): Result<OrderRecord> {
        if (order.id.isBlank()) return Result.failure(IllegalArgumentException("ცარიელი შეკვეთის ID"))
        if (!checkIsCurrentUserAdmin() || auth.currentUser == null) {
            return Result.failure(
                IllegalStateException("PERMISSION_DENIED: გადახდის სტატუსის შესაცვლელად საჭიროა ვერიფიცირებული ადმინისტრატორის ავტორიზაცია (beqa1994@gmail.com)")
            )
        }
        val adminUid = auth.currentUser?.uid ?: "sole_admin"
        val nowMillis = System.currentTimeMillis()
        val prevStatus = order.paymentStatus
        val docRef = db.collection("orders").document(order.id)

        return try {
            val updates = mutableMapOf<String, Any>(
                "paymentStatus" to newPaymentStatus.code,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (newPaymentStatus == PaymentStatus.PAID) {
                updates["paymentConfirmedAtMillis"] = nowMillis
                updates["paymentConfirmedBy"] = adminUid
                updates["priceVerified"] = true
            }
            if (internalNote.isNotBlank()) {
                updates["paymentInternalNote"] = internalNote.trim().take(500)
            }
            docRef.update(updates).await()

            val numericAmount = order.totalAmount.filter { it.isDigit() || it == '.' }.toDoubleOrNull() ?: 0.0
            val auditId = "audit_${nowMillis}_${order.id.takeLast(6)}"
            val auditPayload = hashMapOf<String, Any>(
                "id" to auditId,
                "orderId" to order.id,
                "orderNumber" to order.orderNumber,
                "eventType" to "PAYMENT_STATUS_CHANGED",
                "previousStatus" to prevStatus,
                "newStatus" to newPaymentStatus.code,
                "amount" to numericAmount,
                "currency" to "GEL",
                "actorUid" to adminUid,
                "actorEmail" to AuthAndOrderManager.SOLE_ADMIN_EMAIL,
                "actorRole" to "sole_admin",
                "timestampMillis" to nowMillis,
                "note" to internalNote.trim().take(500),
                "createdAt" to FieldValue.serverTimestamp()
            )
            runCatching {
                db.collection("payment_audit_logs").document(auditId).set(auditPayload).await()
            }

            val updatedRecord = order.copy(
                paymentStatus = newPaymentStatus.code,
                paymentConfirmedAtMillis = if (newPaymentStatus == PaymentStatus.PAID) nowMillis else order.paymentConfirmedAtMillis,
                paymentConfirmedBy = if (newPaymentStatus == PaymentStatus.PAID) adminUid else order.paymentConfirmedBy,
                paymentInternalNote = internalNote.trim().ifBlank { order.paymentInternalNote }
            )
            appContext?.let { ctx ->
                AuthAndOrderManager.updateOrderPaymentStatusLocalAsAdmin(ctx, order, newPaymentStatus, internalNote)
            }
            Result.success(updatedRecord)
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
        val rawExplicitId = (doc.get("productId") as? Number)?.toInt()
            ?: doc.id.removePrefix("prod_").toIntOrNull()
            ?: kotlin.math.abs(doc.id.hashCode()).let { if (it == 0) 9999 else it }

        val shortDesc = doc.getString("shortDescription") ?: ""
        val fullDesc = doc.getString("description") ?: ""
        val subcat = doc.getString("subcategory") ?: ""
        val rawBrand = doc.getString("brand") ?: ""
        val rawBrandId = doc.getString("brandId") ?: ""
        val inStock = doc.getBoolean("inStock") ?: doc.getBoolean("isAvailable") ?: true
        val isActive = doc.getBoolean("isActive") ?: doc.getBoolean("active") ?: true
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
        val rawCategoryId = doc.getString("categoryId") ?: ""
        val resolvedCategoryName = AdminStorage.resolveCanonicalCategoryName(category, productType, rawCategoryId)
        val resolvedCategoryId = AdminStorage.resolveCanonicalCategoryId(resolvedCategoryName, productType, rawCategoryId)
        val condition = doc.getString("condition") ?: "ახალი"
        val exactModel = doc.getString("exactModel") ?: ""
        val originalPrice = doc.getString("originalPrice") ?: ""
        val discountedPrice = doc.getString("discountedPrice") ?: ""
        val discountPercent = (doc.get("discountPercent") as? Number)?.toInt() ?: 0
        val discountExpiresAt = (doc.get("discountExpiresAt") as? Number)?.toLong()?.takeIf { it > 0L }

        val resolvedBrand = AdminStorage.formatCanonicalBrandDisplayName(
            rawBrand.ifBlank { if (title.contains("iPhone", ignoreCase = true)) "Apple" else "Samsung" }
        )
        val resolvedBrandId = AdminStorage.resolveCanonicalBrandId(resolvedBrand, rawBrandId)

        // თუ ადმინმა ადრე შექმნა ფიზიკური აპარატურული პროდუქტი, რომელსაც შეცდომით მიენიჭა ID 86..124 დიაპაზონიდან,
        // გადავიყვანოთ უსაფრთხო ID-ზე (200 + rawExplicitId), ხოლო რეალური აკრძალული სერვისები დავბლოკოთ.
        val probeForHardwareCheck = ProductItem(
            id = if (rawExplicitId in ProductRepository.RETIRED_PRODUCT_IDS) rawExplicitId + 200 else rawExplicitId,
            title = title,
            price = price,
            category = resolvedCategoryName,
            categoryId = resolvedCategoryId,
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
            brand = resolvedBrand,
            brandId = resolvedBrandId,
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
            exactModel = exactModel,
            originalPrice = originalPrice,
            discountedPrice = discountedPrice,
            discountPercent = discountPercent,
            discountExpiresAt = discountExpiresAt,
            isActive = isActive,
            availabilityStatus = doc.getString("availabilityStatus")?.takeIf { it.isNotBlank() } ?: if (inStock) "IN_STOCK" else "OUT_OF_STOCK",
            availabilityLabel = doc.getString("availabilityLabel") ?: ""
        )
        if (ProductRepository.isProhibitedOrRetiredService(probeForHardwareCheck)) {
            return null
        }
        return probeForHardwareCheck
    }

    private fun extractProductIdFromDoc(doc: DocumentSnapshot): Int {
        val raw = (doc.get("productId") as? Number)?.toInt()
            ?: doc.id.removePrefix("prod_").toIntOrNull()
            ?: kotlin.math.abs(doc.id.hashCode()).let { if (it == 0) 9999 else it }
        return if (raw in ProductRepository.RETIRED_PRODUCT_IDS && parseProductDocument(doc) != null) {
            raw + 200
        } else {
            raw
        }
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
                    removedIds.addAll(ProductRepository.RETIRED_PRODUCT_IDS)
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
                        } else {
                            removedIds.add(extractProductIdFromDoc(doc))
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

    /**
     * Fetches a single active product directly from Firestore using its stable Firestore document ID
     * (preferred), SKU field, or numeric productId fallback. Returns null if deleted, inactive, or absent.
     */
    suspend fun fetchProductByStableId(
        productDocId: String = "",
        numericProductId: Int = 0
    ): Result<ProductItem?> {
        val cleanDocId = productDocId.trim()
        if (cleanDocId.isBlank() && numericProductId <= 0) {
            return Result.success(null)
        }
        return try {
            val candidateDocs = mutableListOf<DocumentSnapshot>()
            if (cleanDocId.isNotBlank()) {
                val directSnap = db.collection("products").document(cleanDocId).get().await()
                if (directSnap.exists()) {
                    candidateDocs.add(directSnap)
                } else {
                    val bySkuSnap = db.collection("products")
                        .whereEqualTo("sku", cleanDocId)
                        .limit(1)
                        .get()
                        .await()
                    candidateDocs.addAll(bySkuSnap.documents)
                }
            }
            if (candidateDocs.isEmpty() && numericProductId > 0) {
                val deterministicSnap = db.collection("products").document("prod_$numericProductId").get().await()
                if (deterministicSnap.exists()) {
                    candidateDocs.add(deterministicSnap)
                } else {
                    val byNumericFieldSnap = db.collection("products")
                        .whereEqualTo("productId", numericProductId)
                        .limit(1)
                        .get()
                        .await()
                    candidateDocs.addAll(byNumericFieldSnap.documents)
                }
            }
            val matchedDoc = candidateDocs.firstOrNull() ?: return Result.success(null)
            val parsed = parseProductDocument(matchedDoc)
            if (parsed == null || !parsed.isActive) {
                Result.success(null)
            } else {
                Result.success(parsed)
            }
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, "products/$cleanDocId")
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
            "category" to AdminStorage.resolveCanonicalCategoryName(item.category, item.productType, item.categoryId).take(100),
            "categoryId" to AdminStorage.resolveCanonicalCategoryId(item.category, item.productType, item.categoryId).take(100),
            "type" to item.type.take(50),
            "subcategory" to item.subcategory.take(150),
            "shortDescription" to item.shortDescription.take(1000),
            "description" to item.description.take(5000),
            "brand" to AdminStorage.formatCanonicalBrandDisplayName(item.brand).take(100),
            "brandId" to AdminStorage.resolveCanonicalBrandId(item.brand, item.brandId).take(100),
            "inStock" to item.inStock,
            "isAvailable" to item.inStock,
            "isActive" to item.isActive,
            "active" to item.isActive,
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
        payload["originalPrice"] = item.originalPrice.take(50)
        payload["discountedPrice"] = item.discountedPrice.take(50)
        payload["discountPercent"] = item.discountPercent
        if (item.discountExpiresAt != null && item.discountExpiresAt > 0L) {
            payload["discountExpiresAt"] = item.discountExpiresAt
        } else {
            payload.remove("discountExpiresAt")
        }
        payload["availabilityStatus"] = item.availabilityStatus.take(50)
        payload["availabilityLabel"] = item.availabilityLabel.take(100)
        if (includeCreatedAt) {
            payload["createdAt"] = FieldValue.serverTimestamp()
        }
        return payload
    }

    suspend fun saveOrUpdateProduct(item: ProductItem): String {
        if (ProductRepository.isProhibitedOrRetiredService(item)) {
            throw IllegalArgumentException("PROHIBITED_PRODUCT: ციფრული გამოწერები, ანგარიშები და განბლოკვის სერვისები დაუშვებელია Google Play პოლიტიკით.")
        }
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
            val sanitizedProducts = products.filterNot { ProductRepository.isProhibitedOrRetiredService(it) }
            val uniqueByDocId = sanitizedProducts.distinctBy { it.firestoreDocId.ifBlank { "prod_${it.id}" } }
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
     * - ადარებს აპის სრულ კატალოგს (85 აპარატურული პროდუქტი) და Firestore-ის `products` კოლექციას;
     * - თუ ვერიფიცირებული ადმინი ავტორიზებულია, ავტომატურად ასუფთავებს Cloud-იდან მოძველებულ/დაუშვებელ
     *   პროდუქტებს (ID 86..124), `cat_software` კატეგორიას და ძველ სარეკლამო ბანერებს;
     * - არასდროს გადააწერს Firestore-ში უკვე არსებულ ლეგიტიმურ დოკუმენტებს ავტომატურად;
     * - ტვირთავს მხოლოდ ნაკლულ აპარატურულ პროდუქტებს.
     */
    suspend fun performSafeInitialMigrationIfNeeded(context: Context): Result<MigrationOutcome> =
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val isAdmin = checkIsCurrentUserAdmin()
                val snapshot = db.collection("products").get().await()

                if (isAdmin && auth.currentUser != null) {
                    val retiredDocs = snapshot.documents.filter { doc ->
                        parseProductDocument(doc) == null
                    }
                    if (retiredDocs.isNotEmpty()) {
                        for (chunk in retiredDocs.chunked(400)) {
                            val purgeBatch = db.batch()
                            for (doc in chunk) {
                                purgeBatch.delete(doc.reference)
                            }
                            runCatching { purgeBatch.commit().await() }
                        }
                    }
                    runCatching {
                        db.collection("categories").document("cat_software").delete().await()
                    }
                    runCatching {
                        val bannersSnap = db.collection("app_config").document("home_banners").get().await()
                        if (bannersSnap.exists()) {
                            val rawList = bannersSnap.get("banners") as? List<*>
                            if (rawList != null) {
                                val parsedBanners = rawList.mapNotNull { entry ->
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
                                if (parsedBanners.any { ProductRepository.isProhibitedBanner(it) }) {
                                    val cleanBanners = parsedBanners.filterNot { ProductRepository.isProhibitedBanner(it) }
                                        .ifEmpty { AdminStorage.DEFAULT_HOME_BANNERS }
                                    saveHomeBannersToCloud(cleanBanners)
                                }
                            }
                        }
                    }
                }

                val existingRemote = snapshot.documents.mapNotNull { parseProductDocument(it) }
                val existingDocIds = existingRemote.map { it.firestoreDocId.ifBlank { "prod_${it.id}" } }.toSet()
                val existingProductIds = existingRemote.map { it.id }.toSet()

                val deletedIds = AdminStorage.getDeletedProductIds(context) + ProductRepository.RETIRED_PRODUCT_IDS
                val knownCloudDocIds = AdminStorage.getKnownCloudDocIds(context)
                val localCatalog = AdminStorage.loadProducts(context)

                val missingProducts = localCatalog.filter { item ->
                    val docId = item.firestoreDocId.ifBlank { "prod_${item.id}" }
                    !ProductRepository.isProhibitedOrRetiredService(item) &&
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
        val sanitized = banners.filterNot { ProductRepository.isProhibitedBanner(it) }
        val docRef = db.collection("app_config").document("home_banners")
        val bannersData = sanitized.mapIndexed { index, b ->
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
                    }.filterNot { ProductRepository.isProhibitedBanner(it) }
                    if (parsed.isNotEmpty()) {
                        onBannersUpdated(parsed)
                    } else {
                        onBannersUpdated(AdminStorage.DEFAULT_HOME_BANNERS)
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
                                bogIban = bogIban,
                                bogRecipient = bogRecipient,
                                tbcIban = tbcIban,
                                tbcRecipient = tbcRecipient
                            )
                        )
                    }
                }
            }
    }

    suspend fun fetchBankRequisitesFromCloud(): Result<AdminStorage.BankRequisites> {
        return try {
            val snapshot = db.collection("app_config").document("bank_requisites").get().await()
            if (snapshot != null && snapshot.exists()) {
                val bogIban = snapshot.getString("bogIban")?.trim().orEmpty()
                val bogRecipient = snapshot.getString("bogRecipient")?.trim().orEmpty()
                val tbcIban = snapshot.getString("tbcIban")?.trim().orEmpty()
                val tbcRecipient = snapshot.getString("tbcRecipient")?.trim().orEmpty()
                Result.success(
                    AdminStorage.BankRequisites(
                        bogIban = bogIban,
                        bogRecipient = bogRecipient,
                        tbcIban = tbcIban,
                        tbcRecipient = tbcRecipient
                    )
                )
            } else {
                Result.failure(NoSuchElementException("bank_requisites not found"))
            }
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, "app_config/bank_requisites")
            Result.failure(e)
        }
    }

    // ==========================================================================
    // MULTI-BANK PAYMENT ACCOUNTS & IMMUTABLE AUDIT LOGS (/payment_bank_accounts & /payment_audit_logs)
    // ==========================================================================
    suspend fun savePaymentBankAccountToCloud(account: BankAccountConfig): Result<Unit> {
        if (!checkIsCurrentUserAdmin() || auth.currentUser == null) {
            return Result.failure(
                IllegalStateException("PERMISSION_DENIED: საბანკო ანგარიშის Cloud-ში შესანახად საჭიროა ადმინისტრატორის ავტორიზაცია (beqa1994@gmail.com)")
            )
        }
        val docRef = db.collection("payment_bank_accounts").document(account.id)
        val payload = hashMapOf<String, Any>(
            "id" to account.id.take(80),
            "bankCode" to account.bankCode.take(40),
            "bankName" to account.bankName.take(120),
            "recipientName" to account.recipientName.take(150),
            "iban" to BstorePaymentManager.normalizeIban(account.iban).take(40),
            "currency" to "GEL",
            "description" to account.description.take(300),
            "paymentInstructions" to account.paymentInstructions.take(1000),
            "enabled" to account.isEnabled,
            "isEnabled" to account.isEnabled,
            "sortOrder" to account.displayOrder,
            "displayOrder" to account.displayOrder,
            "createdAtMillis" to account.createdAtMillis,
            "updatedAtMillis" to System.currentTimeMillis(),
            "updatedBy" to AuthAndOrderManager.SOLE_ADMIN_EMAIL,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.set(payload).await()
            runCatching {
                val allEnabled = fetchPaymentBankAccountsFromCloud().getOrNull().orEmpty().filter { it.isEnabled }
                val bog = allEnabled.firstOrNull { it.bankCode.equals("bog", ignoreCase = true) }
                val tbc = allEnabled.firstOrNull { it.bankCode.equals("tbc", ignoreCase = true) }
                saveBankRequisitesToCloud(
                    AdminStorage.BankRequisites(
                        bogIban = bog?.iban.orEmpty(),
                        bogRecipient = bog?.recipientName.orEmpty(),
                        tbcIban = tbc?.iban.orEmpty(),
                        tbcRecipient = tbc?.recipientName.orEmpty()
                    )
                )
            }
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    private fun parseBankAccountDoc(doc: DocumentSnapshot): BankAccountConfig? {
        if (!doc.exists()) return null
        val iban = BstorePaymentManager.normalizeIban(doc.getString("iban").orEmpty())
        val bankName = doc.getString("bankName")?.trim().orEmpty()
        val recipient = doc.getString("recipientName")?.trim().orEmpty()
        if (bankName.isBlank() || recipient.isBlank() || iban.isBlank()) return null
        val enabledFlag = doc.getBoolean("enabled") ?: doc.getBoolean("isEnabled") ?: true
        val orderNum = (doc.get("sortOrder") as? Number)?.toInt()
            ?: (doc.get("displayOrder") as? Number)?.toInt()
            ?: 1
        return BankAccountConfig(
            id = doc.getString("id")?.ifBlank { doc.id } ?: doc.id,
            bankCode = doc.getString("bankCode")?.trim().orEmpty().ifBlank { "bog" },
            bankName = bankName,
            recipientName = recipient,
            iban = iban,
            description = doc.getString("description")?.trim().orEmpty(),
            paymentInstructions = doc.getString("paymentInstructions")?.trim().orEmpty()
                .ifBlank { BankAccountConfig.DEFAULT_PAYMENT_INSTRUCTIONS_KA },
            isEnabled = enabledFlag,
            displayOrder = orderNum,
            createdAtMillis = (doc.get("createdAtMillis") as? Number)?.toLong() ?: System.currentTimeMillis(),
            updatedAtMillis = (doc.get("updatedAtMillis") as? Number)?.toLong() ?: System.currentTimeMillis(),
            updatedBy = doc.getString("updatedBy")?.trim().orEmpty().ifBlank { AuthAndOrderManager.SOLE_ADMIN_EMAIL }
        )
    }

    suspend fun fetchPaymentBankAccountsFromCloud(): Result<List<BankAccountConfig>> {
        if (auth.currentUser == null) return Result.failure(IllegalStateException("User not signed in"))
        return try {
            val snapshot = db.collection("payment_bank_accounts").get().await()
            val accounts = snapshot.documents
                .mapNotNull { parseBankAccountDoc(it) }
                .sortedWith(compareBy({ it.displayOrder }, { it.bankName }))
            Result.success(accounts)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, "payment_bank_accounts")
            Result.failure(e)
        }
    }

    fun listenToPaymentBankAccounts(
        onUpdated: (List<BankAccountConfig>) -> Unit
    ): ListenerRegistration? {
        if (auth.currentUser == null) return null
        return db.collection("payment_bank_accounts")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    handleFirestoreError(error, OperationType.LIST, "payment_bank_accounts")
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val parsed = snapshot.documents
                        .mapNotNull { parseBankAccountDoc(it) }
                        .sortedWith(compareBy({ it.displayOrder }, { it.bankName }))
                    onUpdated(parsed)
                }
            }
    }

    suspend fun fetchPaymentAuditLogsForAdmin(): Result<List<PaymentAuditEvent>> {
        if (!checkIsCurrentUserAdmin() || auth.currentUser == null) {
            return Result.failure(
                IllegalStateException("PERMISSION_DENIED: გადახდების აუდიტის ჟურნალი ხელმისაწვდომია მხოლოდ ადმინისტრატორისთვის")
            )
        }
        return try {
            val snapshot = db.collection("payment_audit_logs").get().await()
            val events = snapshot.documents.mapNotNull { doc ->
                if (!doc.exists()) return@mapNotNull null
                PaymentAuditEvent(
                    id = doc.getString("id")?.ifBlank { doc.id } ?: doc.id,
                    orderId = doc.getString("orderId").orEmpty(),
                    orderNumber = doc.getString("orderNumber").orEmpty(),
                    eventType = doc.getString("eventType").orEmpty().ifBlank { "PAYMENT_STATUS_CHANGED" },
                    previousStatus = doc.getString("previousStatus").orEmpty(),
                    newStatus = doc.getString("newStatus").orEmpty(),
                    amount = (doc.get("amount") as? Number)?.toDouble() ?: 0.0,
                    currency = doc.getString("currency").orEmpty().ifBlank { "GEL" },
                    actorUid = doc.getString("actorUid").orEmpty(),
                    actorEmail = doc.getString("actorEmail").orEmpty(),
                    actorRole = doc.getString("actorRole").orEmpty().ifBlank { "sole_admin" },
                    timestampMillis = (doc.get("timestampMillis") as? Number)?.toLong()
                        ?: (doc.get("timestamp") as? Number)?.toLong()
                        ?: doc.getTimestamp("createdAt")?.toDate()?.time
                        ?: System.currentTimeMillis(),
                    note = doc.getString("note").orEmpty()
                )
            }.sortedByDescending { it.timestampMillis }
            Result.success(events)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, "payment_audit_logs")
            Result.failure(e)
        }
    }

    // ==========================================================================
    // APP CONFIG: CONTACTS & SOCIAL MEDIA MANAGEMENT (app_config/contacts)
    // ==========================================================================
    suspend fun saveContactConfigToCloud(config: ContactAndSocialConfig): Result<Unit> {
        if (!checkIsCurrentUserAdmin() || auth.currentUser == null) {
            return Result.failure(
                IllegalStateException("PERMISSION_DENIED: კონტაქტების Cloud-ში შესანახად საჭიროა ადმინისტრატორის ავტორიზაცია (beqa1994@gmail.com)")
            )
        }
        val docRef = db.collection("app_config").document("contacts")
        val phonesData = config.phoneNumbers.map { it.toMap() }
        val socialsData = config.socialLinks.map { it.toMap() }
        val payload = hashMapOf<String, Any>(
            "configType" to "contacts",
            "phoneNumbers" to phonesData,
            "socialLinks" to socialsData,
            "serviceAddress" to config.serviceAddress,
            "workingHours" to config.workingHours,
            "updatedAt" to FieldValue.serverTimestamp(),
            "updatedBy" to (auth.currentUser?.email ?: "beqa1994@gmail.com")
        )
        return try {
            docRef.set(payload).await()
            Log.d("FirestoreRepo", "Contact & social config synced to Firestore app_config/contacts")
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun fetchContactConfigFromCloud(): Result<ContactAndSocialConfig> {
        val docRef = db.collection("app_config").document("contacts")
        return try {
            val snapshot = docRef.get().await()
            if (!snapshot.exists()) {
                return Result.success(ContactAndSocialManager.DEFAULT_CONFIG)
            }
            val rawPhones = snapshot.get("phoneNumbers") as? List<*> ?: emptyList<Any>()
            val parsedPhones = rawPhones.mapNotNull { item ->
                (item as? Map<*, *>)?.let { ContactPhoneNumber.fromMap(it) }
            }
            val rawSocials = snapshot.get("socialLinks") as? List<*> ?: emptyList<Any>()
            val parsedSocials = rawSocials.mapNotNull { item ->
                (item as? Map<*, *>)?.let { SocialMediaLink.fromMap(it) }
            }
            val cfg = ContactAndSocialConfig(
                phoneNumbers = if (parsedPhones.isNotEmpty()) parsedPhones.sortedBy { it.displayOrder } else ContactAndSocialManager.DEFAULT_PHONE_NUMBERS,
                socialLinks = if (parsedSocials.isNotEmpty()) parsedSocials.sortedBy { it.displayOrder } else ContactAndSocialManager.DEFAULT_SOCIAL_LINKS,
                serviceAddress = snapshot.getString("serviceAddress")?.ifBlank { AppBrandConfig.SERVICE_CENTER_ADDRESS_KA } ?: AppBrandConfig.SERVICE_CENTER_ADDRESS_KA,
                workingHours = snapshot.getString("workingHours")?.ifBlank { AppBrandConfig.WORKING_HOURS_KA } ?: AppBrandConfig.WORKING_HOURS_KA,
                updatedAtMillis = snapshot.getTimestamp("updatedAt")?.toDate()?.time ?: System.currentTimeMillis(),
                updatedBy = snapshot.getString("updatedBy") ?: "beqa1994@gmail.com"
            )
            Result.success(cfg)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            Result.failure(e)
        }
    }

    fun listenToContactConfig(onConfigUpdated: (ContactAndSocialConfig) -> Unit): ListenerRegistration {
        return db.collection("app_config").document("contacts")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    handleFirestoreError(error, OperationType.GET, "app_config/contacts")
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val rawPhones = snapshot.get("phoneNumbers") as? List<*> ?: emptyList<Any>()
                    val parsedPhones = rawPhones.mapNotNull { item ->
                        (item as? Map<*, *>)?.let { ContactPhoneNumber.fromMap(it) }
                    }
                    val rawSocials = snapshot.get("socialLinks") as? List<*> ?: emptyList<Any>()
                    val parsedSocials = rawSocials.mapNotNull { item ->
                        (item as? Map<*, *>)?.let { SocialMediaLink.fromMap(it) }
                    }
                    val cfg = ContactAndSocialConfig(
                        phoneNumbers = if (parsedPhones.isNotEmpty()) parsedPhones.sortedBy { it.displayOrder } else ContactAndSocialManager.DEFAULT_PHONE_NUMBERS,
                        socialLinks = if (parsedSocials.isNotEmpty()) parsedSocials.sortedBy { it.displayOrder } else ContactAndSocialManager.DEFAULT_SOCIAL_LINKS,
                        serviceAddress = snapshot.getString("serviceAddress")?.ifBlank { AppBrandConfig.SERVICE_CENTER_ADDRESS_KA } ?: AppBrandConfig.SERVICE_CENTER_ADDRESS_KA,
                        workingHours = snapshot.getString("workingHours")?.ifBlank { AppBrandConfig.WORKING_HOURS_KA } ?: AppBrandConfig.WORKING_HOURS_KA,
                        updatedAtMillis = snapshot.getTimestamp("updatedAt")?.toDate()?.time ?: System.currentTimeMillis(),
                        updatedBy = snapshot.getString("updatedBy") ?: "beqa1994@gmail.com"
                    )
                    onConfigUpdated(cfg)
                }
            }
    }

    // ==========================================================================
    // APP CONFIG: SERVICE CENTER LOCATION (app_config/service_center_location)
    // ==========================================================================
    suspend fun saveServiceCenterLocation(config: ServiceCenterLocationConfig): Result<Unit> {
        if (!checkIsCurrentUserAdmin() || auth.currentUser == null) {
            return Result.failure(
                IllegalStateException("PERMISSION_DENIED: სერვისცენტრის ლოკაციის შესანახად საჭიროა ადმინისტრატორის ავტორიზაცია (beqa1994@gmail.com)")
            )
        }
        val docRef = db.collection("app_config").document("service_center_location")
        val payload = config.toMap()
        return try {
            docRef.set(payload).await()
            Log.d("FirestoreRepo", "Service center location synced to Firestore app_config/service_center_location")
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun fetchServiceCenterLocation(): Result<ServiceCenterLocationConfig> {
        val docRef = db.collection("app_config").document("service_center_location")
        return try {
            val snapshot = docRef.get().await()
            if (!snapshot.exists()) {
                return Result.success(ServiceCenterLocationConfig.DEFAULT)
            }
            val data = snapshot.data ?: emptyMap<String, Any>()
            val cfg = ServiceCenterLocationConfig.fromMap(data)
            Result.success(cfg)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            Result.failure(e)
        }
    }

    fun listenToServiceCenterLocation(onLocationUpdated: (ServiceCenterLocationConfig) -> Unit): ListenerRegistration {
        return db.collection("app_config").document("service_center_location")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    handleFirestoreError(error, OperationType.GET, "app_config/service_center_location")
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val data = snapshot.data ?: emptyMap<String, Any>()
                    val cfg = ServiceCenterLocationConfig.fromMap(data)
                    onLocationUpdated(cfg)
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

    private fun computeTokenSha256Hex(raw: String): String {
        return try {
            val digest = java.security.MessageDigest.getInstance("SHA-256")
                .digest(raw.toByteArray(Charsets.UTF_8))
            digest.joinToString("") { "%02x".format(it) }
        } catch (_: Exception) {
            kotlin.math.abs(raw.hashCode()).toString(16)
        }
    }

    private suspend fun invokeCloudFunctionCallable(
        functionName: String,
        payloadData: JSONObject,
        idToken: String? = null
    ): Result<JSONObject> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val projectId = runCatching { db.app.options.projectId }.getOrNull().orEmpty()
        if (projectId.isBlank() || projectId == "demo-no-project") {
            return@withContext Result.success(JSONObject())
        }
        val endpoint = "https://us-central1-$projectId.cloudfunctions.net/$functionName"
        val connection = (java.net.URL(endpoint).openConnection() as java.net.HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 3500
            readTimeout = 3500
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            setRequestProperty("Accept", "application/json")
            if (!idToken.isNullOrBlank()) {
                setRequestProperty("Authorization", "Bearer $idToken")
            }
        }
        try {
            val envelope = JSONObject().put("data", payloadData)
            connection.outputStream.use { os ->
                os.write(envelope.toString().toByteArray(Charsets.UTF_8))
            }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code in 200..299) {
                val json = runCatching { JSONObject(body) }.getOrElse { JSONObject() }
                val resObj = json.optJSONObject("result") ?: json.optJSONObject("data") ?: json
                Result.success(resObj)
            } else {
                Result.failure(IllegalStateException("Callable $functionName failed with HTTP $code: $body"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            connection.disconnect()
        }
    }

    suspend fun claimFcmTokenOnBackend(
        token: String,
        deviceId: String,
        deviceModel: String = android.os.Build.MODEL ?: "Android",
        notificationsEnabled: Boolean = true,
        preFetchedIdToken: String? = null
    ): Result<Unit> {
        val cleanToken = token.trim()
        if (cleanToken.length < 10 || cleanToken.startsWith("fcm_admin_dev_")) {
            return Result.failure(IllegalArgumentException("Invalid or synthetic FCM token"))
        }
        if (AuthAndOrderManager.isLoggingOut) {
            return Result.failure(IllegalStateException("Logout in progress"))
        }
        val currentUser = auth.currentUser
            ?: return Result.failure(IllegalStateException("Authentication required to claim FCM token"))
        if (currentUser.isAnonymous) {
            return Result.failure(SecurityException("Anonymous sessions cannot claim customer FCM tokens"))
        }
        val tokenStr = preFetchedIdToken?.takeIf { it.isNotBlank() }
            ?: runCatching { currentUser.getIdToken(false).await().token }.getOrNull().orEmpty()
        if (tokenStr.isBlank()) {
            return Result.failure(IllegalStateException("Missing Firebase ID token"))
        }
        val payload = JSONObject()
            .put("token", cleanToken.take(4096))
            .put("deviceId", deviceId.take(64))
            .put("deviceModel", deviceModel.take(120))
            .put("platform", "android")
            .put("notificationsEnabled", notificationsEnabled)
        return invokeCloudFunctionCallable("claimFcmToken", payload, tokenStr).map { }
    }

    suspend fun releaseFcmTokenOnBackend(
        token: String,
        deviceId: String,
        preFetchedIdToken: String? = null
    ): Result<Unit> {
        val currentUser = auth.currentUser
        val tokenStr = preFetchedIdToken?.takeIf { it.isNotBlank() }
            ?: if (currentUser != null && !currentUser.isAnonymous) {
                runCatching { currentUser.getIdToken(false).await().token }.getOrNull().orEmpty()
            } else {
                ""
            }
        if (tokenStr.isBlank()) {
            return Result.failure(IllegalStateException("No authenticated ID token available for releaseFcmToken"))
        }
        val payload = JSONObject()
            .put("token", token.trim().take(4096))
            .put("deviceId", deviceId.trim().take(64))
        return invokeCloudFunctionCallable("releaseFcmToken", payload, tokenStr).map { }
    }

    suspend fun unbindUnauthenticatedDeviceTokenOnBackend(
        token: String,
        deviceId: String = ""
    ): Result<Unit> {
        val cleanToken = token.trim()
        if (cleanToken.length < 80 || cleanToken.startsWith("fcm_admin_dev_")) {
            return Result.success(Unit)
        }
        val payload = JSONObject()
            .put("token", cleanToken.take(4096))
            .put("deviceId", deviceId.trim().take(64))
        return invokeCloudFunctionCallable("unbindUnauthenticatedDeviceToken", payload, null).map { }
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
        if (AuthAndOrderManager.isLoggingOut) {
            return Result.failure(IllegalStateException("Logout in progress"))
        }
        val currentUser = auth.currentUser ?: return Result.failure(
            IllegalStateException("User must be signed in to save FCM token")
        )
        if (currentUser.isAnonymous) {
            return Result.failure(
                SecurityException("PERMISSION_DENIED: Anonymous Firebase sessions cannot register customer FCM tokens")
            )
        }
        val cleanToken = token.trim()
        if (cleanToken.length < 10 || cleanToken.startsWith("fcm_admin_dev_")) {
            return Result.failure(IllegalArgumentException("Invalid or synthetic FCM token"))
        }
        val uid = currentUser.uid
        val tokenSha = computeTokenSha256Hex(cleanToken)
        val safeDeviceId = sanitizeTokenDeviceId(deviceId, cleanToken)
        val multiDeviceDocId = buildDeviceTokenDocId(uid, deviceId, cleanToken)
        val primaryDocRef = db.collection("fcm_tokens").document(multiDeviceDocId)
        val legacyUserDocRef = db.collection("fcm_tokens").document(uid)
        val payload = hashMapOf<String, Any>(
            "userId" to uid,
            "token" to cleanToken.take(4096),
            "tokenHash" to tokenSha,
            "deviceId" to safeDeviceId,
            "email" to (currentUser.email ?: "").take(150),
            "deviceModel" to deviceModel.take(120),
            "platform" to "android",
            "active" to true,
            "notificationsEnabled" to notificationsEnabled,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            primaryDocRef.set(payload).await()
            runCatching { legacyUserDocRef.set(payload).await() }
            runCatching {
                claimFcmTokenOnBackend(
                    token = cleanToken,
                    deviceId = safeDeviceId,
                    deviceModel = deviceModel,
                    notificationsEnabled = notificationsEnabled
                )
            }
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, primaryDocRef.path)
            Result.failure(e)
        }
    }

    private fun resolveServerNotificationClassification(
        type: String,
        audience: String,
        orderId: String,
        orderNumber: String,
        explicitVisibility: String = "",
        explicitRecipientUid: String = ""
    ): Pair<String, String> {
        val cleanType = type.trim()
        val cleanAud = audience.trim()
        val cleanOrderId = orderId.trim()
        val cleanOrderNum = orderNumber.trim()
        val extractedAudUid = if (cleanAud.startsWith("user:")) cleanAud.removePrefix("user:").trim() else ""
        val recipientUid = explicitRecipientUid.trim().ifBlank { extractedAudUid }

        if (explicitVisibility == "ADMIN_ONLY" || cleanType == "admin_order" || cleanAud == "admin_only") {
            return "ADMIN_ONLY" to ""
        }
        val hasPrivateSignal = explicitVisibility == "PRIVATE_USER" ||
            recipientUid.isNotBlank() ||
            cleanAud.startsWith("user:") ||
            cleanOrderId.isNotBlank() ||
            cleanOrderNum.isNotBlank() ||
            cleanType in setOf(
                "order_update",
                "payment_update",
                "payment_confirmed",
                "payment_instructions",
                "account_update",
                "direct_message"
            )
        if (hasPrivateSignal) {
            return "PRIVATE_USER" to recipientUid
        }
        if (cleanType in setOf("announcement", "product", "product_offer", "service_update", "system_update", "promotion") &&
            (cleanAud == "all" || cleanAud == "all_users")
        ) {
            return "PUBLIC" to ""
        }
        return "ADMIN_ONLY" to ""
    }

    suspend fun sendAdminNotification(
        title: String,
        body: String,
        targetProductId: Int = 0,
        targetTab: Int = 0,
        type: String = "announcement",
        explicitNotificationId: String? = null,
        orderId: String = "",
        orderNumber: String = "",
        audience: String = "all",
        visibility: String = "",
        recipientUid: String = "",
        relatedEntityId: String = "",
        targetProductDocId: String = ""
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
        val safeType = if (type in listOf(
                "announcement", "product", "product_offer", "service_update", "order_update",
                "product_request", "payment_update", "payment_confirmed", "payment_instructions",
                "account_update", "direct_message", "system_update", "app_update", "promotion", "admin_order"
            )
        ) type else "announcement"
        val cleanProductDocId = targetProductDocId.trim().take(128)
        if (safeType == "product" && cleanProductDocId.isBlank() && targetProductId <= 0) {
            return Result.failure(IllegalArgumentException("პროდუქტის შეტყობინებისთვის კონკრეტული პროდუქტის არჩევა სავალდებულოა"))
        }
        val cleanOrderId = orderId.trim().take(80)
        val cleanOrderNumber = orderNumber.trim().take(40)
        val cleanAudience = audience.trim().ifBlank { "all" }.take(140)
        val (resolvedVisibility, resolvedRecipientUid) = resolveServerNotificationClassification(
            type = safeType,
            audience = cleanAudience,
            orderId = cleanOrderId,
            orderNumber = cleanOrderNumber,
            explicitVisibility = visibility.trim(),
            explicitRecipientUid = recipientUid.trim()
        )
        if (resolvedVisibility == "PRIVATE_USER" && resolvedRecipientUid.isBlank()) {
            return Result.failure(
                IllegalArgumentException("PRIVATE_USER შეტყობინებისთვის recipientUid სავალდებულოა")
            )
        }
        val normalizedAudience = when (resolvedVisibility) {
            "PRIVATE_USER" -> "user:$resolvedRecipientUid"
            "ADMIN_ONLY" -> "admin_only"
            else -> if (cleanAudience == "all_users") "all_users" else "all"
        }
        val payload = hashMapOf<String, Any>(
            "id" to notifId,
            "title" to cleanTitle,
            "body" to cleanBody,
            "type" to safeType,
            "visibility" to resolvedVisibility,
            "targetProductId" to targetProductId.coerceAtLeast(0),
            "targetProductDocId" to cleanProductDocId,
            "targetTab" to targetTab.coerceIn(0, 7),
            "orderId" to if (resolvedVisibility == "PUBLIC") "" else cleanOrderId,
            "orderNumber" to if (resolvedVisibility == "PUBLIC") "" else cleanOrderNumber,
            "relatedEntityId" to relatedEntityId.trim().ifBlank { cleanOrderId.ifBlank { cleanProductDocId } }.take(120),
            "senderEmail" to (auth.currentUser?.email ?: "").take(150),
            "audience" to normalizedAudience,
            "status" to "submitted",
            "isRead" to false,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (resolvedVisibility == "PRIVATE_USER" && resolvedRecipientUid.isNotBlank()) {
            payload["recipientUid"] = resolvedRecipientUid.take(128)
        }
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
        rebroadcastNow: Boolean = true,
        orderId: String = "",
        orderNumber: String = "",
        audience: String = "all",
        visibility: String = "",
        recipientUid: String = "",
        targetProductDocId: String = ""
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
        val safeType = if (type in listOf(
                "announcement", "product", "product_offer", "service_update", "order_update",
                "product_request", "payment_update", "payment_confirmed", "payment_instructions",
                "account_update", "direct_message", "system_update", "app_update", "promotion", "admin_order"
            )
        ) type else "announcement"
        val cleanProductDocId = targetProductDocId.trim().take(128)
        if (safeType == "product" && cleanProductDocId.isBlank() && targetProductId <= 0) {
            return Result.failure(IllegalArgumentException("პროდუქტის შეტყობინებისთვის კონკრეტული პროდუქტის არჩევა სავალდებულოა"))
        }
        val cleanOrderId = orderId.trim().take(80)
        val cleanOrderNumber = orderNumber.trim().take(40)
        val cleanAudience = audience.trim().ifBlank { "all" }.take(140)
        val (resolvedVisibility, resolvedRecipientUid) = resolveServerNotificationClassification(
            type = safeType,
            audience = cleanAudience,
            orderId = cleanOrderId,
            orderNumber = cleanOrderNumber,
            explicitVisibility = visibility.trim(),
            explicitRecipientUid = recipientUid.trim()
        )
        if (resolvedVisibility == "PRIVATE_USER" && resolvedRecipientUid.isBlank()) {
            return Result.failure(
                IllegalArgumentException("PRIVATE_USER შეტყობინებისთვის recipientUid სავალდებულოა")
            )
        }
        val normalizedAudience = when (resolvedVisibility) {
            "PRIVATE_USER" -> "user:$resolvedRecipientUid"
            "ADMIN_ONLY" -> "admin_only"
            else -> if (cleanAudience == "all_users") "all_users" else "all"
        }
        val payload = hashMapOf<String, Any>(
            "id" to cleanId,
            "title" to cleanTitle,
            "body" to cleanBody,
            "type" to safeType,
            "visibility" to resolvedVisibility,
            "targetProductId" to targetProductId.coerceAtLeast(0),
            "targetProductDocId" to cleanProductDocId,
            "targetTab" to targetTab.coerceIn(0, 7),
            "orderId" to if (resolvedVisibility == "PUBLIC") "" else cleanOrderId,
            "orderNumber" to if (resolvedVisibility == "PUBLIC") "" else cleanOrderNumber,
            "senderEmail" to (auth.currentUser?.email ?: "").take(150),
            "audience" to normalizedAudience,
            "status" to "submitted",
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (resolvedVisibility == "PRIVATE_USER" && resolvedRecipientUid.isNotBlank()) {
            payload["recipientUid"] = resolvedRecipientUid.take(128)
        }
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

    suspend fun markUserNotificationReadInCloud(notificationId: String): Result<Unit> {
        val currentUser = auth.currentUser
        if (notificationId.isBlank() || currentUser == null || currentUser.isAnonymous) {
            return Result.success(Unit)
        }
        return try {
            db.collection("notifications").document(notificationId)
                .update(
                    mapOf(
                        "isRead" to true,
                        "readAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
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

    private fun parsePushNotificationDoc(doc: DocumentSnapshot): PushNotificationRecord? {
        return try {
            val id = (doc.getString("id") ?: doc.id).trim()
            val title = (doc.getString("title") ?: (doc.get("title") as? String))?.trim() ?: return null
            val body = (doc.getString("body") ?: (doc.get("body") as? String))?.trim() ?: return null
            if (id.isBlank() || title.isBlank() || body.isBlank() ||
                BekaNotificationManager.isSyntheticOrTestNotification(id, title, body)
            ) {
                return null
            }
            val type = doc.getString("type") ?: (doc.get("type") as? String) ?: "announcement"
            val targetProductId = doc.getLong("targetProductId")?.toInt()
                ?: (doc.get("targetProductId") as? Number)?.toInt() ?: 0
            val targetProductDocId = (doc.getString("targetProductDocId")
                ?: doc.getString("productDocId")
                ?: "").trim()
            val targetTab = doc.getLong("targetTab")?.toInt()
                ?: (doc.get("targetTab") as? Number)?.toInt() ?: 0
            val senderEmail = doc.getString("senderEmail") ?: ""
            val audience = doc.getString("audience") ?: "all"
            val status = doc.getString("status") ?: "submitted"
            val orderId = doc.getString("orderId") ?: ""
            val orderNumber = doc.getString("orderNumber") ?: ""
            val visibility = doc.getString("visibility")?.trim().orEmpty()
            val recipientUid = doc.getString("recipientUid")?.trim().orEmpty()
                .ifBlank { if (audience.startsWith("user:")) audience.removePrefix("user:").trim() else "" }
            val relatedEntityId = doc.getString("relatedEntityId")?.trim().orEmpty()
            val isRead = doc.getBoolean("isRead") ?: false
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
                status = status,
                orderId = orderId,
                orderNumber = orderNumber,
                visibility = visibility,
                recipientUid = recipientUid,
                relatedEntityId = relatedEntityId,
                isRead = isRead,
                targetProductDocId = targetProductDocId
            )
        } catch (e: Exception) {
            android.util.Log.w("BekaPush", "Skipping unparseable notification doc: ${doc.id}", e)
            null
        }
    }

    suspend fun getBroadcastNotifications(): Result<List<PushNotificationRecord>> {
        return try {
            val isAdmin = checkIsCurrentUserAdmin()
            val query = if (isAdmin) {
                db.collection("notifications").limit(100)
            } else {
                db.collection("notifications").whereEqualTo("visibility", "PUBLIC").limit(100)
            }
            val snapshot = query.get().await()
            val items = snapshot.documents
                .mapNotNull { parsePushNotificationDoc(it) }
                .let { list ->
                    if (isAdmin) list else list.filter {
                        BekaNotificationManager.classifyRecord(it) == NotificationVisibility.PUBLIC
                    }
                }
                .sortedByDescending { it.createdAtMillis }
            Result.success(items)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, "notifications")
            Result.failure(e)
        }
    }

    fun listenToPublicNotifications(
        onError: (String) -> Unit = {},
        onNotificationsUpdated: (List<PushNotificationRecord>) -> Unit
    ): ListenerRegistration {
        return db.collection("notifications")
            .whereEqualTo("visibility", "PUBLIC")
            .limit(100)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    handleFirestoreError(error, OperationType.LIST, "notifications")
                    onError(formatUserFriendlyFirestoreError(error))
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val items = snapshot.documents
                        .mapNotNull { parsePushNotificationDoc(it) }
                        .filter { BekaNotificationManager.classifyRecord(it) == NotificationVisibility.PUBLIC }
                        .sortedByDescending { it.createdAtMillis }
                    onNotificationsUpdated(items)
                }
            }
    }

    fun listenToUserPrivateNotifications(
        uid: String,
        onError: (String) -> Unit = {},
        onNotificationsUpdated: (List<PushNotificationRecord>) -> Unit
    ): ListenerRegistration? {
        val cleanUid = uid.trim()
        val currentUser = auth.currentUser
        if (cleanUid.isBlank() || currentUser == null || currentUser.isAnonymous || currentUser.uid != cleanUid) {
            return null
        }
        return db.collection("notifications")
            .whereEqualTo("recipientUid", cleanUid)
            .limit(100)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    handleFirestoreError(error, OperationType.LIST, "notifications")
                    onError(formatUserFriendlyFirestoreError(error))
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val activeNow = auth.currentUser
                    if (activeNow == null || activeNow.isAnonymous || activeNow.uid != cleanUid) {
                        onNotificationsUpdated(emptyList())
                        return@addSnapshotListener
                    }
                    val items = snapshot.documents
                        .mapNotNull { parsePushNotificationDoc(it) }
                        .filter { rec ->
                            BekaNotificationManager.classifyRecord(rec) == NotificationVisibility.PRIVATE_USER &&
                                (rec.recipientUid == cleanUid || rec.audience == "user:$cleanUid")
                        }
                        .sortedByDescending { it.createdAtMillis }
                    onNotificationsUpdated(items)
                }
            }
    }

    fun listenToAllNotificationsForAdmin(
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
                    val items = snapshot.documents
                        .mapNotNull { parsePushNotificationDoc(it) }
                        .sortedByDescending { it.createdAtMillis }
                    onNotificationsUpdated(items)
                }
            }
    }

    fun listenToBroadcastNotifications(
        onError: (String) -> Unit = {},
        onNotificationsUpdated: (List<PushNotificationRecord>) -> Unit
    ): ListenerRegistration {
        return listenToPublicNotifications(
            onError = onError,
            onNotificationsUpdated = onNotificationsUpdated
        )
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
                            val rawName = doc.getString("name") ?: ""
                            val rawProdType = doc.getString("productType") ?: ""
                            val normName = AdminStorage.resolveCanonicalCategoryName(rawName, rawProdType, doc.id)
                            val meta = AdminStorage.inferCategoryMetadata(normName)
                            val canonicalId = if (doc.id == "cat_audio" || (meta.id.startsWith("cat_") && !meta.id.startsWith("cat_custom_"))) {
                                meta.id
                            } else {
                                doc.id
                            }
                            CategoryItem(
                                id = canonicalId,
                                name = normName,
                                slug = (doc.getString("slug") ?: "").ifBlank { meta.slug },
                                productType = rawProdType.ifBlank { meta.productType },
                                localizedName = (doc.getString("localizedName") ?: "").ifBlank { meta.localizedName },
                                description = (doc.getString("description") ?: "").ifBlank { meta.description },
                                icon = (doc.getString("icon") ?: "").ifBlank { meta.icon },
                                imageUrl = doc.getString("imageUrl") ?: "",
                                displayOrder = (doc.get("displayOrder") as? Number)?.toInt() ?: 0,
                                isActive = doc.getBoolean("isActive") ?: true,
                                isVisible = doc.getBoolean("isVisible") ?: true,
                                isPopular = doc.getBoolean("isPopular") ?: false,
                                popularOrder = (doc.get("popularOrder") as? Number)?.toInt() ?: 0,
                                categoryType = (doc.getString("categoryType") ?: "").ifBlank { meta.categoryType },
                                createdAt = doc.getTimestamp("createdAt")?.toDate()?.time ?: System.currentTimeMillis(),
                                updatedAt = doc.getTimestamp("updatedAt")?.toDate()?.time ?: System.currentTimeMillis()
                            )
                        } catch (_: Exception) {
                            null
                        }
                    }.filter { it.name.isNotBlank() && !ProductRepository.isProhibitedCategory(it) }
                    if (list.isNotEmpty()) {
                        onCategoriesUpdated(list.distinctBy { it.name.lowercase() }.sortedBy { it.displayOrder })
                    }
                }
            }
    }

    suspend fun saveCategory(category: CategoryItem): Result<String> {
        val normName = AdminStorage.resolveCanonicalCategoryName(category.name, category.productType, category.id)
        val meta = AdminStorage.inferCategoryMetadata(normName)
        val normalizedCat = category.copy(
            id = if (category.id == "cat_audio") "cat_headphones" else category.id.ifBlank { meta.id },
            name = normName,
            slug = category.slug.ifBlank { meta.slug },
            productType = category.productType.ifBlank { meta.productType }
        )
        if (normName.isBlank() || ProductRepository.isProhibitedCategory(normalizedCat)) {
            return Result.failure(IllegalArgumentException("PROHIBITED_CATEGORY: კატეგორია დაუშვებელია."))
        }
        if (!checkIsCurrentUserAdmin() || auth.currentUser == null) {
            return Result.failure(IllegalStateException("PERMISSION_DENIED: კატეგორიის შესანახად საჭიროა ადმინისტრატორი (beqa1994@gmail.com)"))
        }
        val docId = normalizedCat.id.ifBlank { meta.id }
        val payload = hashMapOf<String, Any>(
            "id" to docId,
            "name" to normalizedCat.name.take(100),
            "slug" to normalizedCat.slug.take(100),
            "productType" to normalizedCat.productType.take(50),
            "localizedName" to normalizedCat.localizedName.trim().take(100),
            "description" to normalizedCat.description.trim().take(500),
            "icon" to normalizedCat.icon.trim().take(20),
            "imageUrl" to normalizedCat.imageUrl.trim().take(2000),
            "displayOrder" to normalizedCat.displayOrder,
            "sortOrder" to normalizedCat.displayOrder,
            "isActive" to normalizedCat.isActive,
            "isVisible" to normalizedCat.isVisible,
            "isPopular" to normalizedCat.isPopular,
            "popularOrder" to normalizedCat.popularOrder,
            "categoryType" to normalizedCat.categoryType.trim().take(50),
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
    // DYNAMIC BRANDS MANAGEMENT (/brands/{brandId})
    // ==========================================================================
    private fun parseBrandDoc(doc: DocumentSnapshot): BrandItem? {
        if (!doc.exists()) return null
        val rawName = doc.getString("name")?.trim().orEmpty()
        if (rawName.isBlank()) return null
        val displayName = AdminStorage.formatCanonicalBrandDisplayName(rawName)
        val normName = AdminStorage.normalizeBrandKey(displayName)
        if (normName.isBlank()) return null
        val slug = doc.getString("slug")?.trim().orEmpty().ifBlank {
            normName.replace(Regex("[^a-z0-9ა-ჰ]+"), "_").trim('_')
        }
        val id = AdminStorage.resolveCanonicalBrandId(displayName, doc.getString("id")?.ifBlank { doc.id } ?: doc.id)
        return BrandItem(
            id = id,
            name = displayName,
            normalizedName = normName,
            slug = slug,
            logoUrl = doc.getString("logoUrl")?.trim().orEmpty(),
            isActive = doc.getBoolean("isActive") ?: doc.getBoolean("active") ?: true,
            sortOrder = (doc.get("sortOrder") as? Number)?.toInt()
                ?: (doc.get("displayOrder") as? Number)?.toInt()
                ?: 100,
            createdAt = doc.getTimestamp("createdAt")?.toDate()?.time ?: System.currentTimeMillis(),
            updatedAt = doc.getTimestamp("updatedAt")?.toDate()?.time ?: System.currentTimeMillis()
        )
    }

    suspend fun fetchAllBrands(): Result<List<BrandItem>> {
        return try {
            val snapshot = db.collection("brands").get().await()
            val list = snapshot.documents
                .mapNotNull { parseBrandDoc(it) }
                .distinctBy { it.normalizedName }
                .sortedWith(compareBy({ it.sortOrder }, { it.name }))
            Result.success(list)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, "brands")
            Result.failure(e)
        }
    }

    fun listenToBrands(onBrandsUpdated: (List<BrandItem>) -> Unit): ListenerRegistration {
        return db.collection("brands")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    handleFirestoreError(error, OperationType.LIST, "brands")
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents
                        .mapNotNull { parseBrandDoc(it) }
                        .distinctBy { it.normalizedName }
                        .sortedWith(compareBy({ it.sortOrder }, { it.name }))
                    if (list.isNotEmpty()) {
                        onBrandsUpdated(list)
                    }
                }
            }
    }

    suspend fun saveBrand(brand: BrandItem): Result<String> {
        val displayName = AdminStorage.formatCanonicalBrandDisplayName(brand.name)
        val normName = AdminStorage.normalizeBrandKey(displayName)
        if (normName.isBlank()) {
            return Result.failure(IllegalArgumentException("INVALID_BRAND: ბრენდის სახელი ცარიელია."))
        }
        if (!checkIsCurrentUserAdmin() || auth.currentUser == null) {
            return Result.failure(IllegalStateException("PERMISSION_DENIED: ბრენდის შესანახად საჭიროა ადმინისტრატორი (beqa1994@gmail.com)"))
        }
        val docId = AdminStorage.resolveCanonicalBrandId(displayName, brand.id)
        val slug = brand.slug.ifBlank { normName.replace(Regex("[^a-z0-9ა-ჰ]+"), "_").trim('_') }
        val payload = hashMapOf<String, Any>(
            "id" to docId,
            "name" to displayName.take(80),
            "normalizedName" to normName.take(80),
            "slug" to slug.take(80),
            "logoUrl" to brand.logoUrl.trim().take(1000),
            "isActive" to brand.isActive,
            "sortOrder" to brand.sortOrder,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            db.collection("brands").document(docId).set(payload, SetOptions.merge()).await()
            Result.success(docId)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "brands/$docId")
            Result.failure(e)
        }
    }

    suspend fun deleteBrand(brandId: String): Result<Unit> {
        if (!checkIsCurrentUserAdmin() || auth.currentUser == null) {
            return Result.failure(IllegalStateException("PERMISSION_DENIED: ბრენდის წასაშლელად საჭიროა ადმინისტრატორი (beqa1994@gmail.com)"))
        }
        return try {
            db.collection("brands").document(brandId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "brands/$brandId")
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
                    val multiDeviceDocId = buildDeviceTokenDocId(uid, safeDeviceId, cleanToken)
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
                    val multiDeviceDocId = buildDeviceTokenDocId(uid, safeDeviceId, cleanToken)
                    runCatching { db.collection("fcm_tokens").document(multiDeviceDocId).delete().await() }
                }
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
        createdByUid: String = "",
        orderNumber: String = "",
        summaryTitle: String = "",
        priceText: String = "",
        isStockInquiry: Boolean = false
    ): Result<AdminOrderNotificationRecord> {
        val cleanOrderId = orderId.trim()
        if (cleanOrderId.isBlank()) {
            return Result.failure(IllegalArgumentException("Order ID cannot be blank"))
        }
        val normalizedType = if (orderType == "imei_check") "imei_check" else "service_order"
        val notificationId = "admin_order_$cleanOrderId"
        val ordNum = orderNumber.trim().ifBlank { "#BS-${cleanOrderId.takeLast(4).uppercase()}" }
        val cleanSummary = BekaNotificationManager.stripSensitiveDataFromPayload(summaryTitle.trim()).take(80)
        val cleanPrice = BekaNotificationManager.stripSensitiveDataFromPayload(priceText.trim()).take(30)
        val rawTitle = if (isStockInquiry) {
            buildString {
                append("📦 პროდუქტის მოთხოვნა $ordNum")
                if (cleanSummary.isNotBlank()) append(" • $cleanSummary")
            }
        } else {
            buildString {
                append("ახალი შეკვეთა $ordNum")
                if (cleanSummary.isNotBlank()) append(" • $cleanSummary")
                if (cleanPrice.isNotBlank()) append(" • $cleanPrice")
            }
        }
        val rawBody = if (isStockInquiry) {
            buildString {
                append("ახალი პოტენციური შეკვეთა $ordNum: მომხმარებელი ითხოვს მარაგის დაზუსტებას")
                if (cleanSummary.isNotBlank()) append(" ($cleanSummary)")
                append(".")
            }
        } else {
            buildString {
                append("შემოვიდა ახალი შეკვეთა $ordNum")
                if (cleanSummary.isNotBlank()) {
                    append(" ($cleanSummary")
                    if (cleanPrice.isNotBlank()) append(" — $cleanPrice")
                    append(")")
                } else if (cleanPrice.isNotBlank()) {
                    append(" ($cleanPrice)")
                }
                append(".")
            }
        }
        val sanitizedTitle = BekaNotificationManager.stripSensitiveDataFromPayload(rawTitle).take(120)
        val sanitizedBody = BekaNotificationManager.stripSensitiveDataFromPayload(rawBody).take(400)
        val deepLink = "bekaservice://admin/orders/service/$cleanOrderId"

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

    /**
     * Permanently deletes an individual admin order notification document from `/admin_notifications`
     * without touching the corresponding `/orders/{orderId}` document.
     */
    suspend fun deleteAdminOrderNotification(orderIdOrNotificationId: String): Result<Unit> {
        val currentUser = auth.currentUser
        if (currentUser == null || !checkIsCurrentUserAdmin()) {
            return Result.failure(
                IllegalStateException("PERMISSION_DENIED: მხოლოდ ავტორიზებულ ადმინისტრატორს შეუძლია შეტყობინების წაშლა.")
            )
        }
        val cleanId = orderIdOrNotificationId.trim()
        if (cleanId.isBlank()) return Result.failure(IllegalArgumentException("Empty notification ID"))
        val notificationId = if (cleanId.startsWith("admin_order_")) cleanId else "admin_order_$cleanId"
        val docRef = db.collection("admin_notifications").document(notificationId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    /**
     * Fetches a single customer order by document ID or orderNumber when opening an admin notification.
     */
    suspend fun fetchOrderByIdForAdmin(orderIdOrNumber: String): Result<OrderRecord?> {
        val currentUser = auth.currentUser
        if (currentUser == null || !checkIsCurrentUserAdmin()) {
            return Result.failure(
                IllegalStateException("PERMISSION_DENIED: მხოლოდ ავტორიზებულ ადმინისტრატორს აქვს წვდომა შეკვეთებზე.")
            )
        }
        val clean = orderIdOrNumber.trim().removePrefix("admin_order_")
        if (clean.isBlank()) return Result.success(null)
        return try {
            val directSnap = db.collection("orders").document(clean).get().await()
            if (directSnap.exists()) {
                val parsed = directSnap.toObject(FirestoreOrder::class.java)
                    ?.let { fo -> if (fo.id.isBlank()) fo.copy(id = directSnap.id) else fo }
                    ?.toOrderRecord()
                return Result.success(parsed)
            }
            val byNumberSnap = db.collection("orders")
                .whereEqualTo("orderNumber", clean)
                .limit(1)
                .get()
                .await()
            val firstDoc = byNumberSnap.documents.firstOrNull()
            val byNum = firstDoc?.toObject(FirestoreOrder::class.java)
                ?.let { fo -> if (fo.id.isBlank()) fo.copy(id = firstDoc.id) else fo }
                ?.toOrderRecord()
            Result.success(byNum)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, "orders/$clean")
            Result.failure(e)
        }
    }

    /**
     * Fetches a customer order or product inquiry by ID or order number, validating ownership.
     */
    suspend fun fetchUserOrderById(orderIdOrNumber: String, activeUid: String): Result<OrderRecord?> {
        val clean = orderIdOrNumber.trim()
        if (clean.isBlank()) return Result.success(null)
        val isSoleAdmin = checkIsCurrentUserAdmin()
        return try {
            val directSnap = db.collection("orders").document(clean).get().await()
            if (directSnap.exists()) {
                val parsed = directSnap.toObject(FirestoreOrder::class.java)
                    ?.let { fo -> if (fo.id.isBlank()) fo.copy(id = directSnap.id) else fo }
                    ?.toOrderRecord()
                if (parsed != null) {
                    val isOwnerOrAdmin = isSoleAdmin || parsed.userId.isBlank() || (activeUid.isNotBlank() && parsed.userId == activeUid)
                    return if (isOwnerOrAdmin) {
                        Result.success(parsed)
                    } else {
                        Result.failure(IllegalAccessException("წვდომა უარყოფილია: ეს შეკვეთა სხვა ანგარიშს ეკუთვნის"))
                    }
                }
            }
            val byNumberSnap = db.collection("orders")
                .whereEqualTo("orderNumber", clean)
                .limit(1)
                .get()
                .await()
            val firstDoc = byNumberSnap.documents.firstOrNull()
            val byNum = firstDoc?.toObject(FirestoreOrder::class.java)
                ?.let { fo -> if (fo.id.isBlank()) fo.copy(id = firstDoc.id) else fo }
                ?.toOrderRecord()
            if (byNum != null) {
                val isOwnerOrAdmin = isSoleAdmin || byNum.userId.isBlank() || (activeUid.isNotBlank() && byNum.userId == activeUid)
                if (isOwnerOrAdmin) {
                    Result.success(byNum)
                } else {
                    Result.failure(IllegalAccessException("წვდომა უარყოფილია: ეს შეკვეთა სხვა ანგარიშს ეკუთვნის"))
                }
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, "orders/$clean")
            Result.failure(e)
        }
    }

    companion object {
        @Volatile
        private var isCacheConfigured = false

        fun sanitizeTokenDeviceId(deviceId: String, token: String = ""): String {
            val cleanToken = token.trim()
            val fallbackHash = if (cleanToken.isNotBlank()) {
                kotlin.math.abs(cleanToken.hashCode()).toString(16)
            } else {
                "default"
            }
            return deviceId.trim()
                .replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
                .ifBlank { "dev_$fallbackHash" }
                .take(64)
        }

        fun buildDeviceTokenDocId(uid: String, deviceId: String, token: String = ""): String {
            val cleanUid = uid.trim().replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(50)
            val safeDev = sanitizeTokenDeviceId(deviceId, token)
            return "${cleanUid}_${safeDev}".take(120)
        }

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
