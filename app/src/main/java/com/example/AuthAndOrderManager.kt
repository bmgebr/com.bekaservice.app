package com.example

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Single authoritative administrator authentication state model.
 * Guarantees a single source of truth across all components and prevents
 * race conditions or conflicting statuses.
 */
enum class AdminAuthState {
    INITIALIZING,
    VERIFYING,
    VERIFIED_ADMIN,
    NOT_ADMIN,
    ERROR
}

data class UserProfile(
    val id: String,
    val name: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val email: String,
    val avatarUrl: String = "",
    val provider: String, // "Google", "Apple", "Email"
    val phone: String = "",
    val address: String = "",
    val deviceModel: String = ""
) {
    val displayName: String
        get() {
            val combined = "$firstName $lastName".trim()
            return if (combined.isNotBlank()) combined else name.trim()
        }
}

data class OrderItemDetail(
    val productTitle: String,
    val quality: String,
    val quantity: Int,
    val price: String,
    val imageUrl: String
)

data class OrderRecord(
    val id: String,
    val orderNumber: String,
    val date: String,
    val items: List<OrderItemDetail>,
    val totalAmount: String,
    val status: String, // "შესრულებული", "მიმდინარე", "გზაშია", "გაუქმებული"
    val paymentMethod: String = "ნაღდი ანგარიშსწორება",
    val trackingCode: String,
    val userId: String = "",
    val userName: String = "",
    val userPhone: String = "",
    val userEmail: String = "",
    val deliveryAddress: String = "",
    val deliveryMethodCode: String = "pickup",
    val deliveryCity: String = "ახალციხე",
    val deliveryFeeText: String = "უფასო (0 ₾)",
    val paymentMethodCode: String = "legacy_unspecified", // "bank_transfer" | "cod" | "legacy_unspecified"
    val paymentStatus: String = "pending", // "pending" | "awaiting_collection" | "paid" | "failed" | "cancelled" | "expired" | "refunded"
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
    val createdAtMillis: Long = 0L,
    val fulfillmentStatusCode: String = ""
) {
    fun isProductStockInquiry(): Boolean {
        return id.startsWith("req_") ||
            orderNumber.startsWith("#REQ-", ignoreCase = true) ||
            paymentMethod.contains("პროდუქტის მოთხოვნა", ignoreCase = true) ||
            items.any { it.quality.contains("მარაგის მოთხოვნა", ignoreCase = true) }
    }
}

/**
 * მომხმარებლის ავტორიზაციის (Firebase Auth + Jetpack Credential Manager Google Sign-In),
 * პროფილის სრული რედაქტირებისა და შეკვეთების ონლაინ/ლოკალური ისტორიის მენეჯერი.
 */
object AuthAndOrderManager {
    private const val PREFS_NAME = "bekaservice_auth_prefs"
    private const val KEY_IS_LOGGED_IN = "is_logged_in"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_USER_NAME = "user_name"
    private const val KEY_USER_FIRST_NAME = "user_first_name"
    private const val KEY_USER_LAST_NAME = "user_last_name"
    private const val KEY_USER_EMAIL = "user_email"
    private const val KEY_USER_AVATAR = "user_avatar"
    private const val KEY_USER_PROVIDER = "user_provider"
    private const val KEY_USER_PHONE = "user_phone"
    private const val KEY_USER_ADDRESS = "user_address"
    private const val KEY_USER_DEVICE_MODEL = "user_device_model"
    private const val KEY_USER_PASSWORD = "user_password"
    private const val KEY_ORDERS_JSON = "user_orders_json"
    private const val KEY_FAVORITES_JSON = "user_favorites_json"
    private const val KEY_GUEST_FAVORITES_JSON = "user_favorites_json_guest"

    val favoritesStateVersion = androidx.compose.runtime.mutableIntStateOf(0)
    val authStateVersion = androidx.compose.runtime.mutableIntStateOf(0)

    private val _adminAuthState = MutableStateFlow(AdminAuthState.INITIALIZING)
    val adminAuthState: StateFlow<AdminAuthState> = _adminAuthState.asStateFlow()

    private val adminVerificationMutex = Mutex()
    @Volatile private var lastVerifiedAdminUid: String? = null
    @Volatile var isLoggingOut: Boolean = false

    private fun getPrefs(context: Context): SharedPreferences {
        return context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private tailrec fun Context.findActivity(): android.app.Activity? = when (this) {
        is android.app.Activity -> this
        is android.content.ContextWrapper -> baseContext.findActivity()
        else -> null
    }

    const val SOLE_ADMIN_EMAIL = "beqa1994@gmail.com"
    private const val KEY_VERIFIED_ADMIN_UID = "verified_sole_admin_uid"
    private const val KEY_ADMIN_CUSTOM_CLAIM_VERIFIED = "verified_sole_admin_custom_claim"

    fun isAnonymousFirebaseSession(): Boolean {
        val fbUser = safeFirebaseUser() ?: return false
        return fbUser.isAnonymous
    }

    fun isRegisteredAuthenticatedUser(context: Context): Boolean {
        if (isLoggingOut) return false
        val fbUser = safeFirebaseUser()
        if (fbUser != null) {
            return !fbUser.isAnonymous
        }
        val prefs = getPrefs(context)
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false) &&
            !prefs.getString(KEY_USER_ID, null).isNullOrBlank()
    }

    fun activeRegisteredUidOrNull(context: Context): String? {
        if (isLoggingOut) return null
        val fbUser = safeFirebaseUser()
        if (fbUser != null) {
            return if (fbUser.isAnonymous) null else fbUser.uid
        }
        val prefs = getPrefs(context)
        if (!prefs.getBoolean(KEY_IS_LOGGED_IN, false)) return null
        return prefs.getString(KEY_USER_ID, null)?.takeIf { it.isNotBlank() }
    }

    fun isGuestSession(context: Context): Boolean = !isRegisteredAuthenticatedUser(context)

    fun isSoleAdminEmail(email: String?): Boolean {
        return email?.trim()?.equals(SOLE_ADMIN_EMAIL, ignoreCase = true) == true
    }

    fun resolveActiveUserEmail(context: Context): String {
        if (isLoggingOut || isAnonymousFirebaseSession()) return ""
        val fbUser = safeFirebaseUser()
        val fbDirect = fbUser?.email?.trim().orEmpty()
        if (fbDirect.isNotBlank()) return fbDirect
        val fbProvider = fbUser?.providerData
            ?.firstNotNullOfOrNull { it.email?.trim()?.takeIf { e -> e.isNotBlank() } }
            .orEmpty()
        if (fbProvider.isNotBlank()) return fbProvider
        return getCurrentUser(context)?.email?.trim().orEmpty()
    }

    /**
     * Checks whether the currently authenticated user is the sole authorized administrator
     * ("beqa1994@gmail.com") verified via Firebase Auth / Google Sign-In credentials.
     * Rejects unauthenticated users, anonymous sessions, regular customers, and unverified client-side email logins.
     */
    fun isCurrentUserVerifiedSoleAdmin(context: Context): Boolean {
        if (isLoggingOut || isAnonymousFirebaseSession()) return false
        val fbUser = safeFirebaseUser()
        if (fbUser != null) {
            if (fbUser.isAnonymous) return false
            val fbEmail = fbUser.email?.trim().orEmpty().ifBlank {
                fbUser.providerData.firstNotNullOfOrNull { it.email?.trim()?.takeIf { e -> e.isNotBlank() } }.orEmpty()
            }
            return isSoleAdminEmail(fbEmail)
        }

        val prefs = getPrefs(context)
        if (!prefs.getBoolean(KEY_IS_LOGGED_IN, false)) {
            return false
        }
        val currentUser = getCurrentUser(context) ?: return false
        if (!isSoleAdminEmail(currentUser.email)) {
            return false
        }
        if (!currentUser.provider.equals("Google", ignoreCase = true)) {
            return false
        }
        val verifiedUid = prefs.getString(KEY_VERIFIED_ADMIN_UID, null)
        return !verifiedUid.isNullOrBlank() && verifiedUid == currentUser.id
    }

    /**
     * Returns true ONLY when the authenticated user is "beqa1994@gmail.com" AND their
     * Firebase Custom Claim (`admin == true` or server-side `/admins/{uid}` authorization)
     * has been verified. Email alone is NEVER sufficient for protected admin operations.
     */
    fun hasVerifiedAdminClaim(context: Context): Boolean {
        if (!isCurrentUserVerifiedSoleAdmin(context)) {
            return false
        }
        if (_adminAuthState.value == AdminAuthState.VERIFIED_ADMIN) {
            return true
        }
        val prefs = getPrefs(context)
        val verified = prefs.getBoolean(KEY_ADMIN_CUSTOM_CLAIM_VERIFIED, false)
        val verifiedUid = prefs.getString(KEY_VERIFIED_ADMIN_UID, null)
        val currentUid = safeFirebaseUser()?.uid ?: getCurrentUser(context)?.id
        val isMatch = verified && !verifiedUid.isNullOrBlank() && verifiedUid == currentUid
        if (isMatch && _adminAuthState.value != AdminAuthState.VERIFIED_ADMIN) {
            _adminAuthState.value = AdminAuthState.VERIFIED_ADMIN
            lastVerifiedAdminUid = currentUid
        }
        return isMatch
    }

    /**
     * Updates the verified custom claim state and notifies UI observers.
     */
    fun setAdminClaimVerified(context: Context, verified: Boolean) {
        val isSoleAdmin = isCurrentUserVerifiedSoleAdmin(context)
        val effectiveVerified = isSoleAdmin && verified
        getPrefs(context).edit()
            .putBoolean(KEY_ADMIN_CUSTOM_CLAIM_VERIFIED, effectiveVerified)
            .commit()
        if (effectiveVerified) {
            val uid = safeFirebaseUser()?.uid ?: getCurrentUser(context)?.id
            lastVerifiedAdminUid = uid
            AdminStorage.setAdminSessionActive(context, true, SOLE_ADMIN_EMAIL)
            _adminAuthState.value = AdminAuthState.VERIFIED_ADMIN
        } else {
            lastVerifiedAdminUid = null
            AdminStorage.clearAdminSession(context)
            _adminAuthState.value = if (isSoleAdmin) AdminAuthState.INITIALIZING else AdminAuthState.NOT_ADMIN
        }
        authStateVersion.intValue++
    }

    fun notifyAuthStateChanged(context: Context) {
        val fbUser = safeFirebaseUser()
        val email = fbUser?.email ?: getCurrentUser(context)?.email
        if (!isSoleAdminEmail(email)) {
            lastVerifiedAdminUid = null
            _adminAuthState.value = AdminAuthState.NOT_ADMIN
        } else {
            val verified = getPrefs(context).getBoolean(KEY_ADMIN_CUSTOM_CLAIM_VERIFIED, false)
            val verifiedUid = getPrefs(context).getString(KEY_VERIFIED_ADMIN_UID, null)
            val currentUid = fbUser?.uid ?: getCurrentUser(context)?.id
            if (verified && !verifiedUid.isNullOrBlank() && verifiedUid == currentUid) {
                lastVerifiedAdminUid = currentUid
                _adminAuthState.value = AdminAuthState.VERIFIED_ADMIN
            }
        }
        authStateVersion.intValue++
    }

    /**
     * Authoritative, thread-safe administrator backend authorization verification.
     * Guaranteed single source of truth:
     * 1. Confirms the authenticated account email is exactly "beqa1994@gmail.com".
     * 2. Inspects the Firebase Auth ID Token custom claims (`claims["admin"] == true`)
     *    and server-side `/admins/{uid}` (`enabled == true`) document.
     * 3. Prevents duplicate concurrent verification jobs per UID using a Mutex.
     * 4. Transitions deterministically: INITIALIZING -> VERIFYING -> VERIFIED_ADMIN (or ERROR / NOT_ADMIN).
     */
    suspend fun verifyAdminBackendAuthorization(context: Context, forceRefresh: Boolean = false): Boolean {
        val currentEmail = resolveActiveUserEmail(context)

        // 1. If caller is not sole admin email, reject immediately and set NOT_ADMIN
        if (!isSoleAdminEmail(currentEmail)) {
            adminVerificationMutex.withLock {
                lastVerifiedAdminUid = null
                _adminAuthState.value = AdminAuthState.NOT_ADMIN
                getPrefs(context).edit()
                    .remove(KEY_VERIFIED_ADMIN_UID)
                    .putBoolean(KEY_ADMIN_CUSTOM_CLAIM_VERIFIED, false)
                    .commit()
                AdminStorage.clearAdminSession(context)
            }
            return false
        }

        val fbUser = safeFirebaseUser()
        val currentUid = fbUser?.uid ?: getCurrentUser(context)?.id.orEmpty()

        // 2. Prevent duplicate work: if already VERIFIED_ADMIN for this UID and not forceRefresh, return immediately
        if (!forceRefresh && _adminAuthState.value == AdminAuthState.VERIFIED_ADMIN && lastVerifiedAdminUid == currentUid) {
            return true
        }

        // 3. Thread-safe Mutex lock ensures only ONE verification runs at a time per UID
        return adminVerificationMutex.withLock {
            if (!forceRefresh && _adminAuthState.value == AdminAuthState.VERIFIED_ADMIN && lastVerifiedAdminUid == currentUid) {
                return@withLock true
            }

            _adminAuthState.value = AdminAuthState.VERIFYING

            val isAuthorized = try {
                if (fbUser != null) {
                    val fbEmail = fbUser.email?.trim().orEmpty().ifBlank {
                        fbUser.providerData.firstNotNullOfOrNull { it.email?.trim()?.takeIf { e -> e.isNotBlank() } }.orEmpty()
                    }
                    if (!isSoleAdminEmail(fbEmail)) {
                        return@withLock false
                    }

                    val projectId = runCatching {
                        com.google.firebase.FirebaseApp.getInstance().options.projectId
                    }.getOrNull().orEmpty()
                    val uid = fbUser.uid
                    val isEmailVerified = fbUser.isEmailVerified

                    Log.i("AdminAuthDiag", "Verifying token for Project: $projectId, UID: $uid, Email: $fbEmail, EmailVerified: $isEmailVerified")

                    // Step 3a: Check initial cached token first (getIdToken(false))
                    var claimAdmin = runCatching {
                        val tokenResult = fbUser.getIdToken(false).await()
                        val tokenUid = (tokenResult.claims["user_id"] as? String)
                            ?: (tokenResult.claims["sub"] as? String)
                            ?: uid
                        val tokenEmail = (tokenResult.claims["email"] as? String)?.trim().orEmpty().ifBlank { fbEmail }
                        val signInProvider = tokenResult.signInProvider
                            ?: ((tokenResult.claims["firebase"] as? Map<*, *>)?.get("sign_in_provider") as? String)
                            ?: ""
                        val emailVerified = (tokenResult.claims["email_verified"] as? Boolean) != false ||
                            isEmailVerified ||
                            signInProvider == "password"
                        val hasClaim = isSoleAdminEmail(tokenEmail) && emailVerified && tokenResult.claims["admin"] == true && tokenUid == uid
                        Log.i("AdminAuthDiag", "Cached Token has admin:true -> $hasClaim")
                        hasClaim
                    }.getOrDefault(false)

                    // Step 3b: If not in cache, force-refresh ID Token ONCE
                    if (!claimAdmin) {
                        claimAdmin = runCatching {
                            val refreshedToken = fbUser.getIdToken(true).await()
                            val refUid = (refreshedToken.claims["user_id"] as? String)
                                ?: (refreshedToken.claims["sub"] as? String)
                                ?: uid
                            val refEmail = (refreshedToken.claims["email"] as? String)?.trim().orEmpty().ifBlank { fbEmail }
                            val signInProvider = refreshedToken.signInProvider
                                ?: ((refreshedToken.claims["firebase"] as? Map<*, *>)?.get("sign_in_provider") as? String)
                                ?: ""
                            val emailVerified = (refreshedToken.claims["email_verified"] as? Boolean) != false ||
                                isEmailVerified ||
                                signInProvider == "password"
                            val hasClaim = isSoleAdminEmail(refEmail) && emailVerified && refreshedToken.claims["admin"] == true && refUid == uid
                            Log.i("AdminAuthDiag", "Refreshed Token has admin:true -> $hasClaim")
                            hasClaim
                        }.getOrDefault(false)
                    }

                    // Step 3c: Check Firestore server document /admins/{uid}
                    val repo = FirestoreRepository(context)
                    val serverDocVerified = runCatching {
                        repo.checkIsCurrentUserAdmin()
                    }.getOrDefault(false)
                    Log.i("AdminAuthDiag", "Firestore document /admins/$uid verification -> $serverDocVerified")

                    claimAdmin || serverDocVerified
                } else {
                    val prefs = getPrefs(context)
                    val currentUser = getCurrentUser(context)
                    val isEmailAdmin = isSoleAdminEmail(currentUser?.email)
                    val hasClaim = prefs.getBoolean(KEY_ADMIN_CUSTOM_CLAIM_VERIFIED, false)
                    isEmailAdmin && hasClaim
                }
            } catch (e: Exception) {
                Log.w("AdminAuthDiag", "Admin authorization verification error: ${e.message}", e)
                false
            }

            if (isAuthorized) {
                lastVerifiedAdminUid = currentUid
                _adminAuthState.value = AdminAuthState.VERIFIED_ADMIN
                getPrefs(context).edit()
                    .putString(KEY_VERIFIED_ADMIN_UID, currentUid)
                    .putBoolean(KEY_ADMIN_CUSTOM_CLAIM_VERIFIED, true)
                    .commit()
                AdminStorage.setAdminSessionActive(context, true, SOLE_ADMIN_EMAIL)
                BekaNotificationManager.onAuthorizedAdminAuthenticated(context)
                true
            } else {
                lastVerifiedAdminUid = null
                _adminAuthState.value = AdminAuthState.ERROR
                getPrefs(context).edit()
                    .putBoolean(KEY_ADMIN_CUSTOM_CLAIM_VERIFIED, false)
                    .commit()
                AdminStorage.clearAdminSession(context)
                false
            }
        }
    }

    suspend fun ensureAdminFirebaseSession(context: Context): Boolean {
        return verifyAdminBackendAuthorization(context)
    }

    // =========================================================================
    // FIREBASE AUTH + JETPACK CREDENTIAL MANAGER (GOOGLE SIGN-IN)
    // =========================================================================
    @Volatile
    private var hasAttemptedAutoCredentialInSession = false

    suspend fun attemptAutoSignIn(context: Context): UserProfile? {
        val firebaseUser = safeFirebaseUser()
        if (firebaseUser != null) {
            if (firebaseUser.isAnonymous) return null
            return syncFirebaseUserToLocal(context, firebaseUser)
        }

        if (hasAttemptedAutoCredentialInSession) {
            return null
        }
        hasAttemptedAutoCredentialInSession = true

        val webClientId = runCatching {
            context.getString(R.string.default_web_client_id)
        }.getOrNull()?.takeIf { it.isNotBlank() } ?: return null

        val activityOrContext = context.findActivity() ?: context
        val credentialManager = CredentialManager.create(activityOrContext)

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(true)
            .setServerClientId(webClientId)
            .setAutoSelectEnabled(true)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        return try {
            val result = credentialManager.getCredential(activityOrContext, request)
            handleSignInResult(context, result.credential)
        } catch (e: GetCredentialCancellationException) {
            Log.w("Auth", "Google Sign-In auto-flow cancelled or dismissed: ${e.message}", e)
            null
        } catch (e: NoCredentialException) {
            Log.d("Auth", "No authorized Google credentials available for silent sign-in")
            null
        } catch (e: Exception) {
            Log.e("Auth", "Auto sign-in failed", e)
            null
        }
    }

    suspend fun signInWithGoogleCredentialManager(
        activityContext: Context,
        onCancel: () -> Unit = {},
        onError: (String) -> Unit = {}
    ): UserProfile? {
        val webClientId = try {
            activityContext.getString(R.string.default_web_client_id)
        } catch (e: Exception) {
            onError("Google Sign-In კონფიგურაცია ვერ მოიძებნა: default_web_client_id არ არსებობს")
            return null
        }

        val targetContext = activityContext.findActivity() ?: activityContext
        val credentialManager = CredentialManager.create(targetContext)

        val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = webClientId).build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(signInOption)
            .build()

        return try {
            val result = credentialManager.getCredential(targetContext, request)
            handleSignInResult(activityContext, result.credential)
        } catch (e: GetCredentialCancellationException) {
            Log.w("Auth", "Google Sign-In flow cancelled or dismissed: ${e.message}", e)
            onCancel()
            null
        } catch (e: Exception) {
            Log.e("Auth", "Google Sign-In failed", e)
            onError(e.localizedMessage ?: "Google ავტორიზაცია ვერ მოხერხდა")
            null
        }
    }

    private suspend fun handleSignInResult(
        context: Context,
        credential: androidx.credentials.Credential
    ): UserProfile? {
        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            val firebaseCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
            val authResult = FirebaseAuth.getInstance().signInWithCredential(firebaseCredential).await()
            val firebaseUser = authResult.user
            if (firebaseUser != null) {
                return syncFirebaseUserToLocal(context, firebaseUser)
            }
        } else {
            Log.e("Auth", "Unexpected credential type: ${credential.type}")
        }
        return null
    }

    private val LEGACY_DEMO_ORDER_IDS = setOf("ord_1048", "ord_1052", "ord_1040")

    private fun userKey(baseKey: String, uid: String): String = "${baseKey}_$uid"

    private const val STORE_DEFAULT_ADDRESS = "ქ. ახალციხე, კოსტავას ქ. 10"
    private const val STORE_DEFAULT_PHONE = "+995 598 23 33 32"

    private fun sanitizeUserAddress(addr: String?): String {
        val trimmed = addr?.trim().orEmpty()
        return if (trimmed == STORE_DEFAULT_ADDRESS) "" else trimmed
    }

    private fun sanitizeUserPhone(phone: String?): String {
        val trimmed = phone?.trim().orEmpty()
        return if (trimmed == STORE_DEFAULT_PHONE) "" else trimmed
    }

    suspend fun refreshCurrentUserFromCloud(context: Context): UserProfile? {
        val firebaseUser = safeFirebaseUser() ?: return getCurrentUser(context)
        if (firebaseUser.isAnonymous) return null
        return syncFirebaseUserToLocal(context, firebaseUser)
    }

    private suspend fun syncFirebaseUserToLocal(
        context: Context,
        firebaseUser: com.google.firebase.auth.FirebaseUser
    ): UserProfile {
        if (firebaseUser.isAnonymous) {
            throw SecurityException("Anonymous Firebase sessions cannot sync as registered customer profiles")
        }
        val uid = firebaseUser.uid
        val prefs = getPrefs(context)
        val previousUid = prefs.getString(KEY_USER_ID, null)
            ?.takeIf { prefs.getBoolean(KEY_IS_LOGGED_IN, false) && it.isNotBlank() }
        val email = firebaseUser.email
            ?: prefs.getString(userKey(KEY_USER_EMAIL, uid), null)
            ?: ""
        val fullName = firebaseUser.displayName?.trim()?.takeIf { it.isNotBlank() }
            ?: prefs.getString(userKey(KEY_USER_NAME, uid), null)?.trim()?.takeIf { it.isNotBlank() }
            ?: ""
        val resolvedProvider = resolveFirebaseProviderLabel(firebaseUser)
        val photo = firebaseUser.photoUrl?.toString()?.takeIf { it.isNotBlank() }
            ?: prefs.getString(userKey(KEY_USER_AVATAR, uid), "")
            ?: ""
        val cachedPhoneForUid = sanitizeUserPhone(prefs.getString(userKey(KEY_USER_PHONE, uid), null))
        val cachedAddressForUid = sanitizeUserAddress(prefs.getString(userKey(KEY_USER_ADDRESS, uid), null))
        val cachedModelForUid = prefs.getString(userKey(KEY_USER_DEVICE_MODEL, uid), null) ?: ""

        val repo = FirestoreRepository(context)
        var fetchSucceeded = false
        val cloudProfile = try {
            val profile = repo.getUserProfile(preferServer = true)
            fetchSucceeded = true
            profile
        } catch (e: Exception) {
            Log.w("AuthAndOrderManager", "Could not fetch cloud profile during sign-in: ${e.message}")
            null
        }

        val resolvedPhone = sanitizeUserPhone(cloudProfile?.phoneNumber).ifBlank { cachedPhoneForUid }
        val resolvedAddress = sanitizeUserAddress(cloudProfile?.address).ifBlank { cachedAddressForUid }
        val resolvedModel = if (cloudProfile != null) {
            cloudProfile.deviceModel
        } else {
            cachedModelForUid
        }
        val resolvedDisplayName = cloudProfile?.displayName?.trim()
            ?.takeIf { it.isNotBlank() && it != "User" && it != "მომხმარებელი" }
            ?: fullName
        val first = resolvedDisplayName.split(" ").firstOrNull().orEmpty()
        val last = resolvedDisplayName.split(" ").drop(1).joinToString(" ")
        val resolvedAvatar = cloudProfile?.photoUrl?.takeIf { it.isNotBlank() } ?: photo

        val user = UserProfile(
            id = uid,
            name = resolvedDisplayName,
            firstName = first,
            lastName = last,
            email = email,
            avatarUrl = resolvedAvatar,
            provider = resolvedProvider,
            phone = resolvedPhone,
            address = resolvedAddress,
            deviceModel = resolvedModel
        )

        // Update local session cache ONLY — never overwrite an existing Firestore user document on login!
        saveLocalSessionOnly(context, user)
        BekaNotificationManager.onUserSessionChanged(context, previousUid, uid)
        if (isSoleAdminEmail(email)) {
            var verifiedClaim = runCatching {
                val tokenResult = firebaseUser.getIdToken(false).await()
                tokenResult.claims["admin"] == true
            }.getOrDefault(false) || runCatching {
                repo.checkIsCurrentUserAdmin()
            }.getOrDefault(false)

            if (!verifiedClaim) {
                // Backend securely ensures correct Firebase UID has "admin: true" and /admins/{uid} doc
                runCatching {
                    repo.syncSoleAdminCustomClaims()
                }
                verifiedClaim = runCatching {
                    val refreshedToken = firebaseUser.getIdToken(true).await()
                    refreshedToken.claims["admin"] == true
                }.getOrDefault(false) || runCatching {
                    repo.checkIsCurrentUserAdmin()
                }.getOrDefault(false)
            }

            prefs.edit()
                .putString(KEY_VERIFIED_ADMIN_UID, uid)
                .putBoolean(KEY_ADMIN_CUSTOM_CLAIM_VERIFIED, verifiedClaim)
                .commit()
            if (verifiedClaim) {
                lastVerifiedAdminUid = uid
                _adminAuthState.value = AdminAuthState.VERIFIED_ADMIN
                AdminStorage.setAdminSessionActive(context, true, SOLE_ADMIN_EMAIL)
                BekaNotificationManager.onAuthorizedAdminAuthenticated(context)
            } else {
                lastVerifiedAdminUid = null
                _adminAuthState.value = AdminAuthState.INITIALIZING
                AdminStorage.clearAdminSession(context)
            }
        } else {
            prefs.edit()
                .remove(KEY_VERIFIED_ADMIN_UID)
                .putBoolean(KEY_ADMIN_CUSTOM_CLAIM_VERIFIED, false)
                .commit()
            lastVerifiedAdminUid = null
            _adminAuthState.value = AdminAuthState.NOT_ADMIN
            AdminStorage.clearAdminSession(context)
            BekaNotificationManager.onNonAdminAuthenticated(context)
        }

        // Merge any guest favorites & cloud favorites into the authenticated account's favorites
        val localAccountFavs = readFavoritesSetFromKey(prefs, userKey(KEY_FAVORITES_JSON, uid))
        val guestFavs = readFavoritesSetFromKey(prefs, KEY_GUEST_FAVORITES_JSON)
        val cloudFavs = cloudProfile?.favoriteProductIds?.filter { it > 0 }?.toSet() ?: emptySet()
        val mergedFavs = (localAccountFavs + guestFavs + cloudFavs)
        writeFavoritesSetToKey(prefs, userKey(KEY_FAVORITES_JSON, uid), mergedFavs)
        if (guestFavs.isNotEmpty()) {
            prefs.edit().remove(KEY_GUEST_FAVORITES_JSON).commit()
        }
        favoritesStateVersion.intValue++
        authStateVersion.intValue++

        // Only create the Firestore user document if server confirmed no document exists yet for this UID
        if (fetchSucceeded && cloudProfile == null) {
            repo.saveUserProfileWithResult(user)
            if (mergedFavs.isNotEmpty()) {
                repo.saveUserFavoritesWithResult(mergedFavs, user)
            }
        } else if (fetchSucceeded && mergedFavs != cloudFavs) {
            repo.saveUserFavoritesWithResult(mergedFavs, user)
        }
        return user
    }

    // =========================================================================
    // ავტორიზაცია & პროფილის მართვა
    // =========================================================================
    fun safeFirebaseUser() = runCatching { FirebaseAuth.getInstance().currentUser }.getOrNull()

    fun getCurrentUser(context: Context): UserProfile? {
        if (isLoggingOut) return null
        val prefs = getPrefs(context)
        val firebaseUser = safeFirebaseUser()
        if (firebaseUser != null && firebaseUser.isAnonymous) return null
        val isLocalLoggedIn = prefs.getBoolean(KEY_IS_LOGGED_IN, false)

        if (firebaseUser == null && !isLocalLoggedIn) return null

        val uid = firebaseUser?.uid ?: prefs.getString(KEY_USER_ID, null) ?: return null
        val isSameCachedUid = prefs.getString(KEY_USER_ID, null) == uid

        val resolvedEmail = firebaseUser?.email
            ?: prefs.getString(userKey(KEY_USER_EMAIL, uid), null)
            ?: (if (isSameCachedUid) prefs.getString(KEY_USER_EMAIL, null) else null)
            ?: ""

        val storedName = prefs.getString(userKey(KEY_USER_NAME, uid), null)?.trim()?.takeIf { it.isNotBlank() }
            ?: (if (isSameCachedUid) prefs.getString(KEY_USER_NAME, null)?.trim()?.takeIf { it.isNotBlank() } else null)
            ?: firebaseUser?.displayName?.trim()?.takeIf { it.isNotBlank() }
            ?: ""
        val first = prefs.getString(userKey(KEY_USER_FIRST_NAME, uid), null)
            ?: (if (isSameCachedUid) prefs.getString(KEY_USER_FIRST_NAME, "") else "")
            ?: ""
        val last = prefs.getString(userKey(KEY_USER_LAST_NAME, uid), null)
            ?: (if (isSameCachedUid) prefs.getString(KEY_USER_LAST_NAME, "") else "")
            ?: ""
        val resolvedFirst = if (first.isNotBlank()) first.trim() else storedName.split(" ").firstOrNull().orEmpty()
        val resolvedLast = if (last.isNotBlank()) last.trim() else storedName.split(" ").drop(1).joinToString(" ")

        val rawPhone = prefs.getString(userKey(KEY_USER_PHONE, uid), null)
            ?: (if (isSameCachedUid) prefs.getString(KEY_USER_PHONE, null) else null)
        val rawAddress = prefs.getString(userKey(KEY_USER_ADDRESS, uid), null)
            ?: (if (isSameCachedUid) prefs.getString(KEY_USER_ADDRESS, null) else null)
        val resolvedPhone = sanitizeUserPhone(rawPhone)
        val resolvedAddress = sanitizeUserAddress(rawAddress)
        val resolvedDeviceModel = prefs.getString(userKey(KEY_USER_DEVICE_MODEL, uid), null)
            ?: (if (isSameCachedUid) prefs.getString(KEY_USER_DEVICE_MODEL, null) else null)
            ?: ""
        val resolvedProvider = if (firebaseUser != null) {
            resolveFirebaseProviderLabel(firebaseUser)
        } else {
            prefs.getString(KEY_USER_PROVIDER, "Email") ?: "Email"
        }

        return UserProfile(
            id = uid,
            name = storedName,
            firstName = resolvedFirst,
            lastName = resolvedLast,
            email = resolvedEmail,
            avatarUrl = prefs.getString(userKey(KEY_USER_AVATAR, uid), null)?.takeIf { it.isNotBlank() }
                ?: firebaseUser?.photoUrl?.toString()
                ?: (if (isSameCachedUid) prefs.getString(KEY_USER_AVATAR, "") else "")
                ?: "",
            provider = resolvedProvider,
            phone = resolvedPhone,
            address = resolvedAddress,
            deviceModel = resolvedDeviceModel
        )
    }

    fun resolveFirebaseProviderLabel(firebaseUser: com.google.firebase.auth.FirebaseUser?): String {
        if (firebaseUser == null) return ""
        val providerIds = firebaseUser.providerData.map { it.providerId }
        return when {
            GoogleAuthProvider.PROVIDER_ID in providerIds -> "Google"
            "apple.com" in providerIds -> "Apple"
            com.google.firebase.auth.EmailAuthProvider.PROVIDER_ID in providerIds -> "Email"
            else -> "Google"
        }
    }

    fun supportsPasswordChangeForCurrentUser(context: Context): Boolean {
        val fbUser = safeFirebaseUser()?.takeIf { !it.isAnonymous }
        if (fbUser != null) {
            return fbUser.providerData.any { it.providerId == com.google.firebase.auth.EmailAuthProvider.PROVIDER_ID }
        }
        val currentUser = getCurrentUser(context) ?: return false
        return currentUser.provider.equals("Email", ignoreCase = true)
    }

    fun loginWithGoogle(
        context: Context,
        email: String,
        name: String = "",
        avatarUrl: String = "",
        firstName: String = "",
        lastName: String = "",
        phone: String = "",
        address: String = "",
        deviceModel: String = "",
        hasAdminClaim: Boolean = true
    ): UserProfile {
        val prefs = getPrefs(context)
        val previousUid = activeRegisteredUidOrNull(context)
        val firebaseUid = safeFirebaseUser()?.takeIf { !it.isAnonymous }?.uid ?: ("google_" + email.lowercase().hashCode())
        val resolvedModel = deviceModel.ifBlank {
            prefs.getString(userKey(KEY_USER_DEVICE_MODEL, firebaseUid), "") ?: ""
        }
        val cleanName = name.trim()
        val user = UserProfile(
            id = firebaseUid,
            name = cleanName,
            firstName = firstName.trim().ifBlank { cleanName.split(" ").firstOrNull().orEmpty() },
            lastName = lastName.trim().ifBlank { cleanName.split(" ").drop(1).joinToString(" ") },
            email = email,
            avatarUrl = avatarUrl,
            provider = "Google",
            phone = sanitizeUserPhone(phone.ifBlank { prefs.getString(userKey(KEY_USER_PHONE, firebaseUid), "") }),
            address = sanitizeUserAddress(address.ifBlank { prefs.getString(userKey(KEY_USER_ADDRESS, firebaseUid), "") }),
            deviceModel = resolvedModel
        )
        saveLocalSessionOnly(context, user)
        BekaNotificationManager.onUserSessionChanged(context, previousUid, firebaseUid)
        if (isSoleAdminEmail(email)) {
            val effectiveClaim = hasAdminClaim && (safeFirebaseUser() == null || hasAdminClaim)
            prefs.edit()
                .putString(KEY_VERIFIED_ADMIN_UID, firebaseUid)
                .putBoolean(KEY_ADMIN_CUSTOM_CLAIM_VERIFIED, effectiveClaim)
                .commit()
            if (effectiveClaim) {
                lastVerifiedAdminUid = firebaseUid
                _adminAuthState.value = AdminAuthState.VERIFIED_ADMIN
                AdminStorage.setAdminSessionActive(context, true, SOLE_ADMIN_EMAIL)
                BekaNotificationManager.onAuthorizedAdminAuthenticated(context)
            } else {
                lastVerifiedAdminUid = null
                _adminAuthState.value = AdminAuthState.INITIALIZING
                AdminStorage.clearAdminSession(context)
            }
            if (safeFirebaseUser() != null) {
                CoroutineScope(Dispatchers.IO).launch {
                    runCatching { ensureAdminFirebaseSession(context) }
                }
            }
        } else {
            prefs.edit()
                .remove(KEY_VERIFIED_ADMIN_UID)
                .putBoolean(KEY_ADMIN_CUSTOM_CLAIM_VERIFIED, false)
                .commit()
            lastVerifiedAdminUid = null
            _adminAuthState.value = AdminAuthState.NOT_ADMIN
            AdminStorage.clearAdminSession(context)
            BekaNotificationManager.onNonAdminAuthenticated(context)
        }
        authStateVersion.intValue++
        return user
    }

    fun loginWithApple(
        context: Context,
        email: String,
        name: String = "",
        firstName: String = "",
        lastName: String = "",
        phone: String = "",
        address: String = "",
        deviceModel: String = ""
    ): UserProfile {
        val prefs = getPrefs(context)
        val previousUid = activeRegisteredUidOrNull(context)
        val appleUid = "apple_" + email.lowercase().hashCode()
        val resolvedModel = deviceModel.ifBlank {
            prefs.getString(userKey(KEY_USER_DEVICE_MODEL, appleUid), "") ?: ""
        }
        val cleanName = name.trim()
        val user = UserProfile(
            id = appleUid,
            name = cleanName,
            firstName = firstName.trim().ifBlank { cleanName.split(" ").firstOrNull().orEmpty() },
            lastName = lastName.trim().ifBlank { cleanName.split(" ").drop(1).joinToString(" ") },
            email = email,
            avatarUrl = "",
            provider = "Apple",
            phone = sanitizeUserPhone(phone.ifBlank { prefs.getString(userKey(KEY_USER_PHONE, appleUid), "") }),
            address = sanitizeUserAddress(address.ifBlank { prefs.getString(userKey(KEY_USER_ADDRESS, appleUid), "") }),
            deviceModel = resolvedModel
        )
        saveLocalSessionOnly(context, user)
        BekaNotificationManager.onUserSessionChanged(context, previousUid, appleUid)
        prefs.edit()
            .remove(KEY_VERIFIED_ADMIN_UID)
            .putBoolean(KEY_ADMIN_CUSTOM_CLAIM_VERIFIED, false)
            .commit()
        lastVerifiedAdminUid = null
        _adminAuthState.value = AdminAuthState.NOT_ADMIN
        AdminStorage.clearAdminSession(context)
        BekaNotificationManager.onNonAdminAuthenticated(context)
        authStateVersion.intValue++
        return user
    }

    fun loginWithEmail(context: Context, email: String, pass: String): Pair<Boolean, String> {
        if (email.isBlank() || !email.contains("@")) {
            return Pair(false, "გთხოვთ შეიყვანოთ სწორი ელ. ფოსტა")
        }
        if (pass.length < 6) {
            return Pair(false, "პაროლი უნდა შედგებოდეს მინიმუმ 6 სიმბოლოსგან")
        }
        if (isSoleAdminEmail(email)) {
            val fbUser = safeFirebaseUser()
            val fbVerifiedAdmin = fbUser != null && !fbUser.isAnonymous && isSoleAdminEmail(fbUser.email)
            if (!fbVerifiedAdmin) {
                AdminStorage.clearAdminSession(context)
                return Pair(
                    false,
                    "ადმინისტრატორის ანგარიშზე შესვლა დასაშვებია მხოლოდ დადასტურებული Google ავტორიზაციით."
                )
            }
        }

        val prefs = getPrefs(context)
        val previousUid = activeRegisteredUidOrNull(context)
        val uid = safeFirebaseUser()?.takeIf { !it.isAnonymous }?.uid ?: ("email_" + email.trim().lowercase().hashCode())
        val savedNameForUid = prefs.getString(userKey(KEY_USER_FIRST_NAME, uid), null)?.trim().orEmpty()
        val firstName = savedNameForUid
        val lastName = prefs.getString(userKey(KEY_USER_LAST_NAME, uid), "")?.trim().orEmpty()
        val fullName = "$firstName $lastName".trim()

        val user = UserProfile(
            id = uid,
            name = fullName,
            firstName = firstName,
            lastName = lastName,
            email = email.trim(),
            avatarUrl = prefs.getString(userKey(KEY_USER_AVATAR, uid), "") ?: "",
            provider = "Email",
            phone = sanitizeUserPhone(prefs.getString(userKey(KEY_USER_PHONE, uid), "")),
            address = sanitizeUserAddress(prefs.getString(userKey(KEY_USER_ADDRESS, uid), "")),
            deviceModel = prefs.getString(userKey(KEY_USER_DEVICE_MODEL, uid), "") ?: ""
        )
        saveLocalSessionOnly(context, user)
        BekaNotificationManager.onUserSessionChanged(context, previousUid, uid)
        if (prefs.getString(KEY_USER_PASSWORD, null) == null) {
            prefs.edit().putString(KEY_USER_PASSWORD, pass).apply()
        }
        prefs.edit()
            .remove(KEY_VERIFIED_ADMIN_UID)
            .putBoolean(KEY_ADMIN_CUSTOM_CLAIM_VERIFIED, false)
            .commit()
        lastVerifiedAdminUid = null
        _adminAuthState.value = AdminAuthState.NOT_ADMIN
        AdminStorage.clearAdminSession(context)
        BekaNotificationManager.onNonAdminAuthenticated(context)
        authStateVersion.intValue++
        return Pair(true, "წარმატებული ავტორიზაცია!")
    }

    fun registerWithEmail(
        context: Context,
        name: String,
        email: String,
        pass: String,
        firstName: String = "",
        lastName: String = "",
        phone: String = "",
        address: String = "",
        deviceModel: String = ""
    ): Pair<Boolean, String> {
        if (name.isBlank() && firstName.isBlank()) return Pair(false, "გთხოვთ მიუთითოთ თქვენი სახელი")
        if (email.isBlank() || !email.contains("@")) return Pair(false, "გთხოვთ შეიყვანოთ სწორი ელ. ფოსტა")
        if (pass.length < 6) return Pair(false, "პაროლი უნდა იყოს მინიმუმ 6 სიმბოლო")
        if (isSoleAdminEmail(email)) {
            AdminStorage.clearAdminSession(context)
            return Pair(
                false,
                "ადმინისტრატორის ანგარიშის რეგისტრაცია დაუშვებელია. გთხოვთ გაიაროთ Google ავტორიზაცია."
            )
        }

        val rFirst = if (firstName.isNotBlank()) firstName else name.split(" ").firstOrNull() ?: name
        val rLast = if (lastName.isNotBlank()) lastName else name.split(" ").drop(1).joinToString(" ")
        val fullName = "$rFirst $rLast".trim()
        val previousUid = activeRegisteredUidOrNull(context)
        val uid = safeFirebaseUser()?.takeIf { !it.isAnonymous }?.uid ?: ("email_" + email.trim().lowercase().hashCode())

        val user = UserProfile(
            id = uid,
            name = fullName,
            firstName = rFirst,
            lastName = rLast,
            email = email.trim(),
            avatarUrl = "",
            provider = "Email",
            phone = sanitizeUserPhone(phone),
            address = sanitizeUserAddress(address),
            deviceModel = deviceModel
        )
        saveUserSession(context, user)
        BekaNotificationManager.onUserSessionChanged(context, previousUid, uid)
        getPrefs(context).edit()
            .putString(KEY_USER_PASSWORD, pass)
            .remove(KEY_VERIFIED_ADMIN_UID)
            .putBoolean(KEY_ADMIN_CUSTOM_CLAIM_VERIFIED, false)
            .commit()
        lastVerifiedAdminUid = null
        _adminAuthState.value = AdminAuthState.NOT_ADMIN
        AdminStorage.clearAdminSession(context)
        BekaNotificationManager.onNonAdminAuthenticated(context)
        authStateVersion.intValue++
        return Pair(true, "რეგისტრაცია წარმატებით დასრულდა!")
    }

    fun updateUserProfile(context: Context, updated: UserProfile): UserProfile {
        saveUserSession(context, updated)
        return updated
    }

    suspend fun saveUserProfileToCloud(context: Context, updated: UserProfile): Result<UserProfile> {
        val fbUser = safeFirebaseUser()?.takeIf { !it.isAnonymous }
        if (fbUser != null) {
            val normalized = updated.copy(id = fbUser.uid)
            val cloudResult = FirestoreRepository(context).saveUserProfileWithResult(normalized)
            return if (cloudResult.isSuccess) {
                saveLocalSessionOnly(context, normalized)
                Result.success(normalized)
            } else {
                Result.failure(
                    cloudResult.exceptionOrNull()
                        ?: IllegalStateException("პროფილის Cloud-ში შენახვა ვერ მოხერხდა")
                )
            }
        } else {
            saveLocalSessionOnly(context, updated)
            return Result.success(updated)
        }
    }

    /**
     * Updates individual contact fields (phone and/or address) on the active user's profile
     * and persists changes to both local session storage and Firebase (/users/{uid}).
     */
    suspend fun updateSingleContactField(
        context: Context,
        phone: String? = null,
        address: String? = null
    ): Result<UserProfile> {
        val existing = getCurrentUser(context)
            ?: return Result.failure(IllegalStateException("მონაცემების შესანახად გთხოვთ გაიაროთ ავტორიზაცია"))
        val newPhone = phone?.trim() ?: existing.phone
        val newAddress = address?.trim() ?: existing.address
        val updated = existing.copy(
            phone = sanitizeUserPhone(newPhone),
            address = sanitizeUserAddress(newAddress)
        )
        return saveUserProfileToCloud(context, updated)
    }

    private suspend fun syncContactFieldsAfterSuccessfulOrder(
        context: Context,
        checkoutConfig: CheckoutSubmissionConfig?
    ) {
        if (checkoutConfig == null) return
        val existing = getCurrentUser(context) ?: return
        val newPhone = checkoutConfig.customerPhone.trim().takeIf { it.length >= 6 }
        val newCourierAddress = if (checkoutConfig.deliveryMethod == DeliveryMethodType.COURIER) {
            checkoutConfig.deliveryAddress.trim().takeIf { it.isNotBlank() }
        } else {
            null
        }
        val effectivePhone = newPhone ?: existing.phone
        val effectiveAddress = newCourierAddress ?: existing.address
        if (effectivePhone != existing.phone || effectiveAddress != existing.address) {
            val updated = existing.copy(
                phone = sanitizeUserPhone(effectivePhone),
                address = sanitizeUserAddress(effectiveAddress)
            )
            runCatching { saveUserProfileToCloud(context, updated) }
        }
    }

    fun changePassword(context: Context, oldPass: String, newPass: String): Pair<Boolean, String> {
        if (!supportsPasswordChangeForCurrentUser(context)) {
            return Pair(false, "პაროლის შეცვლა მიუწვდომელია გარე ავტორიზაციის პროვაიდერისთვის (Google / Apple)")
        }
        val prefs = getPrefs(context)
        val uid = activeRegisteredUidOrNull(context).orEmpty()
        val scopedKey = if (uid.isNotBlank()) userKey(KEY_USER_PASSWORD, uid) else KEY_USER_PASSWORD
        val storedPass = prefs.getString(scopedKey, null) ?: prefs.getString(KEY_USER_PASSWORD, "") ?: ""

        if (storedPass.isNotEmpty() && oldPass != storedPass) {
            return Pair(false, "მიმდინარე პაროლი არასწორია")
        }
        if (newPass.length < 6) {
            return Pair(false, "ახალი პაროლი უნდა შეიცავდეს მინიმუმ 6 სიმბოლოს")
        }

        prefs.edit()
            .putString(scopedKey, newPass)
            .putString(KEY_USER_PASSWORD, newPass)
            .apply()
        return Pair(true, "პაროლი წარმატებით შეიცვალა!")
    }

    suspend fun changePasswordWithFirebaseIfActive(
        context: Context,
        oldPass: String,
        newPass: String
    ): Pair<Boolean, String> {
        if (newPass.length < 6) {
            return Pair(false, "ახალი პაროლი უნდა შეიცავდეს მინიმუმ 6 სიმბოლოს")
        }
        val fbUser = safeFirebaseUser()?.takeIf { !it.isAnonymous }
        if (fbUser != null) {
            val hasPasswordProvider = fbUser.providerData.any {
                it.providerId == com.google.firebase.auth.EmailAuthProvider.PROVIDER_ID
            }
            if (!hasPasswordProvider) {
                return Pair(
                    false,
                    "თქვენი ანგარიში იყენებს ${resolveFirebaseProviderLabel(fbUser)} ავტორიზაციას. პაროლის მართვა ხდება შესაბამისი პროვაიდერის პარამეტრებიდან."
                )
            }
            val email = fbUser.email?.trim().orEmpty()
            if (email.isBlank()) {
                return Pair(false, "ანგარიშის ელ. ფოსტა ვერ მოიძებნა")
            }
            return try {
                val credential = com.google.firebase.auth.EmailAuthProvider.getCredential(email, oldPass)
                fbUser.reauthenticate(credential).await()
                fbUser.updatePassword(newPass).await()
                Pair(true, "პაროლი წარმატებით შეიცვალა!")
            } catch (e: Exception) {
                Pair(false, e.localizedMessage ?: "პაროლის შეცვლა ვერ მოხერხდა")
            }
        }
        return changePassword(context, oldPass, newPass)
    }

    suspend fun sendPasswordResetEmail(email: String): Pair<Boolean, String> {
        val clean = email.trim()
        if (clean.isBlank() || !clean.contains("@")) {
            return Pair(false, "გთხოვთ შეიყვანოთ სწორი ელ. ფოსტა")
        }
        val fbAuth = runCatching { FirebaseAuth.getInstance() }.getOrNull()
            ?: return Pair(false, "Firebase Authentication მიუწვდომელია")
        return try {
            fbAuth.sendPasswordResetEmail(clean).await()
            Pair(true, "პაროლის აღდგენის ბმული გაგზავნილია ელ. ფოსტაზე!")
        } catch (e: Exception) {
            Pair(false, e.localizedMessage ?: "აღდგენის ბმულის გაგზავნა ვერ მოხერხდა")
        }
    }

    fun logout(context: Context) {
        isLoggingOut = true
        try {
            val previousUid = activeRegisteredUidOrNull(context)
            val fbAuth = runCatching { FirebaseAuth.getInstance() }.getOrNull()
            val fbUser = runCatching { fbAuth?.currentUser }.getOrNull()
            val cachedIdToken = runCatching {
                val t = fbUser?.getIdToken(false)
                if (t != null && t.isComplete && t.isSuccessful) t.result?.token else null
            }.getOrNull()

            BekaNotificationManager.cleanupOnUserLogout(
                context = context,
                explicitUid = previousUid ?: fbUser?.uid.orEmpty(),
                preFetchedIdToken = cachedIdToken
            )
            AdminStorage.clearAdminSession(context)

            val hadFirebaseUser = try {
                val hasUser = fbUser != null
                if (hasUser) {
                    fbAuth?.signOut()
                }
                hasUser
            } catch (_: Throwable) {
                false
            }
            if (hadFirebaseUser) {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        kotlinx.coroutines.withTimeoutOrNull(3000L) {
                            CredentialManager.create(context.applicationContext)
                                .clearCredentialState(ClearCredentialStateRequest())
                        }
                    } catch (_: Throwable) {}
                }
            }
            getPrefs(context).edit()
                .putBoolean(KEY_IS_LOGGED_IN, false)
                .remove(KEY_VERIFIED_ADMIN_UID)
                .putBoolean(KEY_ADMIN_CUSTOM_CLAIM_VERIFIED, false)
                .remove(KEY_USER_ID)
                .remove(KEY_USER_NAME)
                .remove(KEY_USER_FIRST_NAME)
                .remove(KEY_USER_LAST_NAME)
                .remove(KEY_USER_EMAIL)
                .remove(KEY_USER_AVATAR)
                .remove(KEY_USER_PROVIDER)
                .remove(KEY_USER_PHONE)
                .remove(KEY_USER_ADDRESS)
                .remove(KEY_USER_DEVICE_MODEL)
                .remove(KEY_GUEST_FAVORITES_JSON)
                .commit()
            lastVerifiedAdminUid = null
            _adminAuthState.value = AdminAuthState.NOT_ADMIN
            favoritesStateVersion.intValue++
            authStateVersion.intValue++
        } finally {
            isLoggingOut = false
        }
    }

    /**
     * Async-safe logout for UI flows: captures Firebase ID token and completes backend FCM token release
     * BEFORE calling `FirebaseAuth.signOut()` to eliminate token-release / sign-out race conditions.
     */
    suspend fun logoutSafely(context: Context) {
        isLoggingOut = true
        try {
            val previousUid = activeRegisteredUidOrNull(context)
            val fbAuth = runCatching { FirebaseAuth.getInstance() }.getOrNull()
            val fbUser = runCatching { fbAuth?.currentUser }.getOrNull()
            val idToken = if (fbUser != null) {
                kotlinx.coroutines.withTimeoutOrNull(2500L) {
                    runCatching { fbUser.getIdToken(false).await().token }.getOrNull()
                }
            } else {
                null
            }
            BekaNotificationManager.cleanupOnUserLogoutSuspend(
                context = context,
                explicitUid = previousUid ?: fbUser?.uid.orEmpty(),
                preFetchedIdToken = idToken
            )
        } finally {
            logout(context)
        }
    }

    /**
     * Google Play Account Deletion Requirement: Deletes all cloud data (Firestore profile & tokens),
     * deletes Firebase Auth account, and clears all local session preferences.
     */
    suspend fun deleteUserAccount(context: Context): Result<Unit> {
        val repoResult = runCatching {
            FirestoreRepository(context).deleteCurrentUserAccount()
        }.getOrElse { Result.failure(it) }

        logoutSafely(context)
        return repoResult
    }

    private fun saveLocalSessionOnly(context: Context, user: UserProfile) {
        val fullName = if (user.name.isNotBlank()) user.name else "${user.firstName} ${user.lastName}".trim()
        val uid = user.id.ifBlank { safeFirebaseUser()?.uid ?: "u1" }
        val prefs = getPrefs(context)
        val guestFavs = readFavoritesSetFromKey(prefs, KEY_GUEST_FAVORITES_JSON)
        if (guestFavs.isNotEmpty()) {
            val existingAccountFavs = readFavoritesSetFromKey(prefs, userKey(KEY_FAVORITES_JSON, uid))
            writeFavoritesSetToKey(prefs, userKey(KEY_FAVORITES_JSON, uid), existingAccountFavs + guestFavs)
            prefs.edit().remove(KEY_GUEST_FAVORITES_JSON).commit()
        }
        prefs.edit()
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .putString(KEY_USER_ID, uid)
            .putString(KEY_USER_NAME, fullName)
            .putString(KEY_USER_FIRST_NAME, user.firstName)
            .putString(KEY_USER_LAST_NAME, user.lastName)
            .putString(KEY_USER_EMAIL, user.email)
            .putString(KEY_USER_AVATAR, user.avatarUrl)
            .putString(KEY_USER_PROVIDER, user.provider)
            .putString(KEY_USER_PHONE, user.phone)
            .putString(KEY_USER_ADDRESS, user.address)
            .putString(KEY_USER_DEVICE_MODEL, user.deviceModel)
            // Also persist under UID-scoped keys so account switching / re-login preserves the user's own fields
            .putString(userKey(KEY_USER_NAME, uid), fullName)
            .putString(userKey(KEY_USER_FIRST_NAME, uid), user.firstName)
            .putString(userKey(KEY_USER_LAST_NAME, uid), user.lastName)
            .putString(userKey(KEY_USER_EMAIL, uid), user.email)
            .putString(userKey(KEY_USER_AVATAR, uid), user.avatarUrl)
            .putString(userKey(KEY_USER_PHONE, uid), user.phone)
            .putString(userKey(KEY_USER_ADDRESS, uid), user.address)
            .putString(userKey(KEY_USER_DEVICE_MODEL, uid), user.deviceModel)
            .commit()
        favoritesStateVersion.intValue++
    }

    private fun saveUserSession(context: Context, user: UserProfile) {
        saveLocalSessionOnly(context, user)

        // Sync user profile to Firestore (no passwords or roles stored in Firestore)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                FirestoreRepository(context).saveUserProfile(user)
            } catch (e: Exception) {
                Log.e("AuthAndOrderManager", "Firestore sync failed", e)
            }
        }
    }

    // =========================================================================
    // შეკვეთების სრული ისტორიის მართვა (მხოლოდ რეალური შეკვეთები, დემო ჩანაწერების გარეშე)
    // =========================================================================
    private fun purgeLegacyDemoOrdersIfPresent(prefs: SharedPreferences) {
        val legacyRaw = prefs.getString(KEY_ORDERS_JSON, null)
        if (legacyRaw != null) {
            prefs.edit().remove(KEY_ORDERS_JSON).commit()
        }
    }

    private fun resolveActiveOrdersStorageKey(context: Context): String? {
        val uid = activeRegisteredUidOrNull(context)
        return uid?.takeIf { it.isNotBlank() }?.let { userKey(KEY_ORDERS_JSON, it) }
    }

    fun getOrders(context: Context): List<OrderRecord> {
        val prefs = getPrefs(context)
        purgeLegacyDemoOrdersIfPresent(prefs)

        val storageKey = resolveActiveOrdersStorageKey(context) ?: return emptyList()
        val jsonStr = prefs.getString(storageKey, null)
        if (jsonStr.isNullOrBlank()) {
            return emptyList()
        }

        return try {
            val list = mutableListOf<OrderRecord>()
            val array = JSONArray(jsonStr)
            var hadDemoOrder = false
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val orderId = obj.getString("id")
                if (orderId in LEGACY_DEMO_ORDER_IDS) {
                    hadDemoOrder = true
                    continue
                }
                val itemsArr = obj.getJSONArray("items")
                val itemsList = mutableListOf<OrderItemDetail>()
                for (j in 0 until itemsArr.length()) {
                    val itObj = itemsArr.getJSONObject(j)
                    itemsList.add(
                        OrderItemDetail(
                            productTitle = itObj.getString("productTitle"),
                            quality = itObj.getString("quality"),
                            quantity = itObj.getInt("quantity"),
                            price = itObj.getString("price"),
                            imageUrl = itObj.getString("imageUrl")
                        )
                    )
                }

                val legacyPaymentLabel = obj.optString("paymentMethod", "ნაღდი ანგარიშსწორება")
                val resolvedMethodType = PaymentMethodType.fromCode(
                    obj.optString("paymentMethodCode", ""),
                    legacyPaymentLabel
                )
                val resolvedPaymentStatus = PaymentStatus.fromCode(
                    obj.optString("paymentStatus", ""),
                    resolvedMethodType
                )
                val ordNum = obj.getString("orderNumber")

                list.add(
                    OrderRecord(
                        id = orderId,
                        orderNumber = ordNum,
                        date = obj.getString("date"),
                        items = itemsList,
                        totalAmount = obj.getString("totalAmount"),
                        status = obj.getString("status"),
                        paymentMethod = legacyPaymentLabel,
                        trackingCode = obj.optString("trackingCode", "BS-TRK-10000"),
                        userId = obj.optString("userId", ""),
                        userName = obj.optString("userName", ""),
                        userPhone = obj.optString("userPhone", ""),
                        userEmail = obj.optString("userEmail", ""),
                        deliveryAddress = obj.optString("deliveryAddress", ""),
                        deliveryMethodCode = obj.optString("deliveryMethodCode", "pickup"),
                        deliveryCity = obj.optString("deliveryCity", "ახალციხე"),
                        deliveryFeeText = obj.optString("deliveryFeeText", "უფასო (0 ₾)"),
                        paymentMethodCode = resolvedMethodType.code,
                        paymentStatus = resolvedPaymentStatus.code,
                        bankAccountId = obj.optString("bankAccountId", ""),
                        bankCode = obj.optString("bankCode", ""),
                        bankName = obj.optString("bankName", ""),
                        recipientNameSnapshot = obj.optString("recipientNameSnapshot", ""),
                        ibanSnapshot = obj.optString("ibanSnapshot", ""),
                        paymentReference = obj.optString("paymentReference", ordNum),
                        paymentInstructionsSnapshot = obj.optString("paymentInstructionsSnapshot", ""),
                        userConfirmedPaymentAtMillis = obj.optLong("userConfirmedPaymentAtMillis", 0L),
                        paymentConfirmedAtMillis = obj.optLong("paymentConfirmedAtMillis", 0L),
                        paymentConfirmedBy = obj.optString("paymentConfirmedBy", ""),
                        paymentInternalNote = obj.optString("paymentInternalNote", ""),
                        acceptedTermsVersion = obj.optString("acceptedTermsVersion", ""),
                        acceptedTermsAtMillis = obj.optLong("acceptedTermsAtMillis", 0L),
                        createdAtMillis = obj.optLong("createdAtMillis", 0L),
                        fulfillmentStatusCode = obj.optString("fulfillmentStatusCode", "")
                    )
                )
            }
            if (hadDemoOrder) {
                saveOrders(context, list)
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun mergeCloudOrdersWithLocal(context: Context, cloudOrders: List<OrderRecord>): List<OrderRecord> {
        val genuineOrders = cloudOrders.filterNot { it.id in LEGACY_DEMO_ORDER_IDS }
        saveOrders(context, genuineOrders)
        return genuineOrders
    }

    private fun saveOrders(context: Context, orders: List<OrderRecord>) {
        val prefs = getPrefs(context)
        purgeLegacyDemoOrdersIfPresent(prefs)
        val storageKey = resolveActiveOrdersStorageKey(context) ?: return

        val array = JSONArray()
        orders.filterNot { it.id in LEGACY_DEMO_ORDER_IDS }.forEach { ord ->
            val obj = JSONObject()
            obj.put("id", ord.id)
            obj.put("orderNumber", ord.orderNumber)
            obj.put("date", ord.date)
            obj.put("totalAmount", ord.totalAmount)
            obj.put("status", ord.status)
            obj.put("paymentMethod", ord.paymentMethod)
            obj.put("trackingCode", ord.trackingCode)
            obj.put("userId", ord.userId)
            obj.put("userName", ord.userName)
            obj.put("userPhone", ord.userPhone)
            obj.put("userEmail", ord.userEmail)
            obj.put("deliveryAddress", ord.deliveryAddress)
            obj.put("deliveryMethodCode", ord.deliveryMethodCode)
            obj.put("deliveryCity", ord.deliveryCity)
            obj.put("deliveryFeeText", ord.deliveryFeeText)
            obj.put("paymentMethodCode", ord.paymentMethodCode)
            obj.put("paymentStatus", ord.paymentStatus)
            obj.put("bankAccountId", ord.bankAccountId)
            obj.put("bankCode", ord.bankCode)
            obj.put("bankName", ord.bankName)
            obj.put("recipientNameSnapshot", ord.recipientNameSnapshot)
            obj.put("ibanSnapshot", ord.ibanSnapshot)
            obj.put("paymentReference", ord.paymentReference)
            obj.put("paymentInstructionsSnapshot", ord.paymentInstructionsSnapshot)
            obj.put("userConfirmedPaymentAtMillis", ord.userConfirmedPaymentAtMillis)
            obj.put("paymentConfirmedAtMillis", ord.paymentConfirmedAtMillis)
            obj.put("paymentConfirmedBy", ord.paymentConfirmedBy)
            obj.put("paymentInternalNote", ord.paymentInternalNote)
            obj.put("acceptedTermsVersion", ord.acceptedTermsVersion)
            obj.put("acceptedTermsAtMillis", ord.acceptedTermsAtMillis)
            obj.put("createdAtMillis", ord.createdAtMillis)
            obj.put("fulfillmentStatusCode", ord.fulfillmentStatusCode)

            val itemsArr = JSONArray()
            ord.items.forEach { it ->
                val itObj = JSONObject()
                itObj.put("productTitle", it.productTitle)
                itObj.put("quality", it.quality)
                itObj.put("quantity", it.quantity)
                itObj.put("price", it.price)
                itObj.put("imageUrl", it.imageUrl)
                itemsArr.put(itObj)
            }
            obj.put("items", itemsArr)
            array.put(obj)
        }
        prefs.edit().putString(storageKey, array.toString()).commit()
    }

    fun buildOrderRecordFromCart(
        context: Context,
        cartItems: List<CartItem>,
        checkoutConfig: CheckoutSubmissionConfig? = null
    ): OrderRecord {
        val currentOrders = getOrders(context)
        val num = (1053 + currentOrders.size)
        val nowMillis = System.currentTimeMillis()
        val now = SimpleDateFormat("dd MMMM, yyyy • HH:mm", Locale.forLanguageTag("ka")).format(Date(nowMillis))
        val user = getCurrentUser(context)
        val currentUid = activeRegisteredUidOrNull(context).orEmpty()

        var total = 0
        val details = cartItems.map { c ->
            val numPrice = c.product.price.filter { it.isDigit() }.toIntOrNull() ?: 0
            total += numPrice * c.quantity
            OrderItemDetail(
                productTitle = c.product.title,
                quality = c.selectedQuality,
                quantity = c.quantity,
                price = c.product.price,
                imageUrl = c.product.imageUrl
            )
        }

        val totalFormatted = if (total > 0) "$total ₾" else "ფასი შეთანხმებით"
        val orderNumber = "#BS-$num"
        val methodType = checkoutConfig?.paymentMethod ?: PaymentMethodType.COD
        val bankAcc = if (methodType == PaymentMethodType.BANK_TRANSFER) {
            checkoutConfig?.selectedBankAccount ?: BstorePaymentManager.getEnabledBankAccounts(context).firstOrNull()
        } else {
            null
        }
        val initialPaymentStatus = if (methodType == PaymentMethodType.BANK_TRANSFER) {
            PaymentStatus.PENDING
        } else {
            PaymentStatus.AWAITING_COLLECTION
        }
        val paymentMethodDisplay = when (methodType) {
            PaymentMethodType.BANK_TRANSFER -> {
                val bName = bankAcc?.bankName ?: "საბანკო გადარიცხვა"
                "საბანკო გადარიცხვა ($bName)"
            }
            PaymentMethodType.COD -> "ნაღდი ანგარიშსწორება (გადაიხადე ნივთის მიღებისას)"
            PaymentMethodType.LEGACY_UNSPECIFIED -> "ნაღდი ანგარიშსწორება"
        }
        val deliveryMethod = checkoutConfig?.deliveryMethod ?: DeliveryMethodType.PICKUP

        val resolvedOrderId = checkoutConfig?.clientOrderId
            ?.trim()
            ?.takeIf { it.isNotBlank() && it.length <= 80 }
            ?: "ord_$nowMillis"

        return OrderRecord(
            id = resolvedOrderId,
            orderNumber = orderNumber,
            date = now,
            items = details,
            totalAmount = totalFormatted,
            status = "მიმდინარე",
            paymentMethod = paymentMethodDisplay,
            trackingCode = "BS-TRK-${(10000..99999).random()}",
            userId = currentUid,
            userName = user?.displayName.orEmpty(),
            userPhone = checkoutConfig?.customerPhone?.ifBlank { user?.phone.orEmpty() } ?: user?.phone.orEmpty(),
            userEmail = user?.email.orEmpty(),
            deliveryAddress = checkoutConfig?.deliveryAddress?.ifBlank { user?.address.orEmpty() } ?: "ქ. ახალციხე, კოსტავას N10",
            deliveryMethodCode = deliveryMethod.code,
            deliveryCity = checkoutConfig?.deliveryCity ?: "ახალციხე",
            deliveryFeeText = deliveryMethod.feeLabelKa,
            paymentMethodCode = methodType.code,
            paymentStatus = initialPaymentStatus.code,
            bankAccountId = bankAcc?.id.orEmpty(),
            bankCode = bankAcc?.bankCode.orEmpty(),
            bankName = bankAcc?.bankName.orEmpty(),
            recipientNameSnapshot = bankAcc?.recipientName.orEmpty(),
            ibanSnapshot = bankAcc?.iban.orEmpty(),
            paymentReference = orderNumber,
            paymentInstructionsSnapshot = bankAcc?.paymentInstructions ?: BankAccountConfig.DEFAULT_PAYMENT_INSTRUCTIONS_KA,
            userConfirmedPaymentAtMillis = 0L,
            paymentConfirmedAtMillis = 0L,
            paymentConfirmedBy = "",
            paymentInternalNote = checkoutConfig?.customerNote.orEmpty(),
            acceptedTermsVersion = checkoutConfig?.acceptedTermsVersion ?: BstorePaymentManager.CURRENT_TERMS_VERSION,
            acceptedTermsAtMillis = checkoutConfig?.acceptedTermsAtMillis ?: nowMillis,
            createdAtMillis = nowMillis,
            fulfillmentStatusCode = "pending"
        )
    }

    suspend fun createAndPersistOrderFromCart(
        context: Context,
        cartItems: List<CartItem>,
        checkoutConfig: CheckoutSubmissionConfig? = null
    ): Result<OrderRecord> {
        if (cartItems.isEmpty()) {
            return Result.failure(IllegalArgumentException("კალათა ცარიელია"))
        }
        if (checkoutConfig?.paymentMethod == PaymentMethodType.BANK_TRANSFER) {
            val enabledBanks = BstorePaymentManager.getEnabledBankAccounts(context)
            val chosen = checkoutConfig.selectedBankAccount ?: enabledBanks.firstOrNull()
            if (chosen == null || !chosen.isEnabled || !BstorePaymentManager.isValidIban(chosen.iban)) {
                return Result.failure(
                    IllegalStateException("არჩეული საბანკო ანგარიში მიუწვდომელია. გთხოვთ აირჩიოთ სხვა ბანკი ან ნაღდი ანგარიშსწორება.")
                )
            }
        }

        val newOrder = buildOrderRecordFromCart(context, cartItems, checkoutConfig)
        val numericAmount = newOrder.totalAmount.filter { it.isDigit() }.toDoubleOrNull() ?: 0.0

        BstorePaymentManager.recordAuditEventLocal(
            context = context,
            event = PaymentAuditEvent(
                id = "audit_init_${newOrder.id}",
                orderId = newOrder.id,
                orderNumber = newOrder.orderNumber,
                eventType = "ORDER_PAYMENT_INITIALIZED",
                previousStatus = "none",
                newStatus = newOrder.paymentStatus,
                amount = numericAmount,
                actorUid = newOrder.userId.ifBlank { "guest" },
                actorEmail = newOrder.userEmail.ifBlank { "customer" },
                actorRole = "customer",
                note = "${newOrder.paymentMethod} • ${newOrder.deliveryAddress}"
            )
        )

        val fbUser = safeFirebaseUser()?.takeIf { !it.isAnonymous }
        return if (fbUser != null) {
            val repo = FirestoreRepository(context)
            val user = getCurrentUser(context)
            val res = repo.createOrderFromCart(newOrder, user)
            if (res.isSuccess) {
                val currentOrders = getOrders(context).toMutableList()
                currentOrders.removeAll { it.id == newOrder.id }
                currentOrders.add(0, newOrder)
                saveOrders(context, currentOrders)
                syncContactFieldsAfterSuccessfulOrder(context, checkoutConfig)
                Result.success(newOrder)
            } else {
                Result.failure(
                    res.exceptionOrNull() ?: IllegalStateException("შეკვეთის შენახვა ვერ მოხერხდა")
                )
            }
        } else {
            val currentOrders = getOrders(context).toMutableList()
            currentOrders.removeAll { it.id == newOrder.id }
            currentOrders.add(0, newOrder)
            saveOrders(context, currentOrders)
            syncContactFieldsAfterSuccessfulOrder(context, checkoutConfig)
            val summaryText = newOrder.items.joinToString(", ") { "${it.productTitle} x${it.quantity}" }
            BekaNotificationManager.recordOrderCreatedForAdminOnly(
                context = context,
                orderId = newOrder.id,
                orderType = "service_order",
                orderNumber = newOrder.orderNumber,
                summaryText = summaryText,
                amountText = newOrder.totalAmount
            )
            Result.success(newOrder)
        }
    }

    /**
     * Submits a customer's stock availability inquiry from the AI Assistant as a potential order
     * / product request (`#REQ-...`). Persists locally + to Firestore (`/orders` and `/admin_notifications`)
     * when Firebase Auth is active so the administrator receives the alert immediately.
     */
    suspend fun submitProductStockInquiry(
        context: Context,
        requestedProduct: String,
        customerName: String,
        customerPhone: String,
        customerNote: String = ""
    ): Result<OrderRecord> {
        val cleanProduct = requestedProduct.trim()
        val cleanPhone = customerPhone.trim()
        val cleanName = customerName.trim().ifBlank {
            getCurrentUser(context)?.displayName?.takeIf { it.isNotBlank() } ?: "მომხმარებელი"
        }
        val cleanNote = customerNote.trim()

        if (cleanProduct.isBlank()) {
            return Result.failure(IllegalArgumentException("მიუთითეთ სასურველი პროდუქტი ან მოდელი"))
        }
        if (cleanPhone.filter { it.isDigit() || it == '+' }.length < 4) {
            return Result.failure(IllegalArgumentException("მიუთითეთ საკონტაქტო ტელეფონის ნომერი"))
        }

        val currentOrders = getOrders(context)
        val num = (2100 + currentOrders.size)
        val nowMillis = System.currentTimeMillis()
        val now = SimpleDateFormat("dd MMMM, yyyy • HH:mm", Locale.forLanguageTag("ka")).format(Date(nowMillis))
        val user = getCurrentUser(context)
        val currentUid = activeRegisteredUidOrNull(context).orEmpty()
        val orderNumber = "#REQ-$num"

        val inquiryOrder = OrderRecord(
            id = "req_$nowMillis",
            orderNumber = orderNumber,
            date = now,
            items = listOf(
                OrderItemDetail(
                    productTitle = cleanProduct,
                    quality = "მარაგის მოთხოვნა • პოტენციური შეკვეთა",
                    quantity = 1,
                    price = "დაზუსტების პროცესში",
                    imageUrl = ""
                )
            ),
            totalAmount = "ფასი შეთანხმებით",
            status = "მიმდინარე",
            paymentMethod = "პროდუქტის მოთხოვნა (მარაგის დაზუსტება)",
            trackingCode = "BS-REQ-${(10000..99999).random()}",
            userId = currentUid,
            userName = cleanName,
            userPhone = cleanPhone,
            userEmail = user?.email.orEmpty(),
            deliveryAddress = user?.address.orEmpty().ifBlank { "დაზუსტდება ზარით / WhatsApp-ით" },
            deliveryMethodCode = DeliveryMethodType.PICKUP.code,
            deliveryCity = "ახალციხე",
            deliveryFeeText = DeliveryMethodType.PICKUP.feeLabelKa,
            paymentMethodCode = PaymentMethodType.LEGACY_UNSPECIFIED.code,
            paymentStatus = PaymentStatus.PENDING.code,
            paymentReference = orderNumber,
            paymentInternalNote = cleanNote,
            acceptedTermsVersion = "stock_inquiry_v1",
            acceptedTermsAtMillis = nowMillis,
            createdAtMillis = nowMillis,
            fulfillmentStatusCode = "pending"
        )

        val fbUser = safeFirebaseUser()?.takeIf { !it.isAnonymous }
        return if (fbUser != null) {
            val res = FirestoreRepository(context).createProductStockInquiryOrder(
                orderRecord = inquiryOrder,
                requestedProduct = cleanProduct,
                customerNote = cleanNote
            )
            if (res.isSuccess) {
                val updatedList = getOrders(context).toMutableList()
                updatedList.removeAll { it.id == inquiryOrder.id }
                updatedList.add(0, inquiryOrder)
                saveOrders(context, updatedList)
                savePhoneAndDeliveryIfBlank(context, cleanPhone, cleanName)
                Result.success(inquiryOrder)
            } else {
                Result.failure(
                    res.exceptionOrNull() ?: IllegalStateException("მოთხოვნის გაგზავნა ვერ მოხერხდა")
                )
            }
        } else {
            val updatedList = getOrders(context).toMutableList()
            updatedList.add(0, inquiryOrder)
            saveOrders(context, updatedList)
            savePhoneAndDeliveryIfBlank(context, cleanPhone, cleanName)
            BekaNotificationManager.recordProductInquiryForAdminOnly(
                context = context,
                orderId = inquiryOrder.id,
                orderNumber = inquiryOrder.orderNumber,
                requestedProduct = cleanProduct,
                customerNote = cleanNote
            )
            Result.success(inquiryOrder)
        }
    }

    private fun savePhoneAndDeliveryIfBlank(context: Context, phone: String, name: String) {
        val prefs = getPrefs(context)
        val editor = prefs.edit()
        if (prefs.getString(KEY_USER_PHONE, "").isNullOrBlank() && phone.isNotBlank()) {
            editor.putString(KEY_USER_PHONE, phone)
        }
        if (prefs.getString(KEY_USER_NAME, "").isNullOrBlank() && name.isNotBlank() && name != "მომხმარებელი") {
            editor.putString(KEY_USER_NAME, name)
        }
        editor.apply()
    }

    fun addOrderFromCart(
        context: Context,
        cartItems: List<CartItem>,
        checkoutConfig: CheckoutSubmissionConfig? = null
    ): OrderRecord {
        val newOrder = buildOrderRecordFromCart(context, cartItems, checkoutConfig)
        val currentOrders = getOrders(context).toMutableList()
        currentOrders.add(0, newOrder)
        saveOrders(context, currentOrders)
        val summaryText = newOrder.items.joinToString(", ") { "${it.productTitle} x${it.quantity}" }
        BekaNotificationManager.recordOrderCreatedForAdminOnly(
            context = context,
            orderId = newOrder.id,
            orderType = "service_order",
            orderNumber = newOrder.orderNumber,
            summaryText = summaryText,
            amountText = newOrder.totalAmount
        )

        val numericAmount = newOrder.totalAmount.filter { it.isDigit() }.toDoubleOrNull() ?: 0.0
        BstorePaymentManager.recordAuditEventLocal(
            context = context,
            event = PaymentAuditEvent(
                id = "audit_init_${newOrder.id}",
                orderId = newOrder.id,
                orderNumber = newOrder.orderNumber,
                eventType = "ORDER_PAYMENT_INITIALIZED",
                previousStatus = "none",
                newStatus = newOrder.paymentStatus,
                amount = numericAmount,
                actorUid = newOrder.userId.ifBlank { "guest" },
                actorEmail = newOrder.userEmail.ifBlank { "customer" },
                actorRole = "customer",
                note = newOrder.paymentMethod
            )
        )

        // Sync snapshot order to Firestore orders/{orderId}
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repo = FirestoreRepository(context)
                val user = getCurrentUser(context)
                repo.createOrderFromCart(newOrder, user)
            } catch (e: Exception) {
                Log.e("AuthAndOrderManager", "Firestore order sync failed", e)
            }
        }

        return newOrder
    }

    fun updateOrderStatus(context: Context, orderId: String, newStatus: String, rawCode: String = ""): Boolean {
        val currentOrders = getOrders(context).toMutableList()
        val index = currentOrders.indexOfFirst { it.id == orderId }
        if (index != -1) {
            // Note: Updating order fulfillment status NEVER automatically marks paymentStatus as paid!
            val existing = currentOrders[index]
            currentOrders[index] = existing.copy(
                status = newStatus,
                fulfillmentStatusCode = rawCode.ifBlank { existing.fulfillmentStatusCode }
            )
            saveOrders(context, currentOrders)
            return true
        }
        return false
    }

    /**
     * Switches the destination bank snapshot locally while a bank_transfer order is still `pending`.
     */
    fun switchOrderBankLocal(
        context: Context,
        orderId: String,
        bank: BankAccountConfig
    ): OrderRecord? {
        val currentOrders = getOrders(context).toMutableList()
        val index = currentOrders.indexOfFirst { it.id == orderId }
        if (index == -1) return null
        val ord = currentOrders[index]
        if (ord.paymentMethodCode != PaymentMethodType.BANK_TRANSFER.code) return null
        if (!ord.paymentStatus.equals(PaymentStatus.PENDING.code, ignoreCase = true)) return null
        if (!bank.isEnabled || !BstorePaymentManager.isValidIban(bank.iban)) return null

        val updated = ord.copy(
            bankAccountId = bank.id,
            bankCode = bank.bankCode,
            bankName = bank.bankName,
            recipientNameSnapshot = bank.recipientName,
            ibanSnapshot = BstorePaymentManager.normalizeIban(bank.iban),
            paymentMethod = "საბანკო გადარიცხვა (${bank.bankName})",
            paymentInstructionsSnapshot = bank.paymentInstructions.ifBlank { BankAccountConfig.DEFAULT_PAYMENT_INSTRUCTIONS_KA }
        )
        currentOrders[index] = updated
        saveOrders(context, currentOrders)
        return updated
    }

    /**
     * Customer declaration that bank transfer was sent.
     * Moves paymentStatus strictly from `pending` -> `verification_pending`. NEVER grants `paid`.
     */
    fun markOrderTransferSentByUserLocal(
        context: Context,
        orderId: String,
        bankOverride: BankAccountConfig? = null
    ): Result<OrderRecord> {
        val currentOrders = getOrders(context).toMutableList()
        val index = currentOrders.indexOfFirst { it.id == orderId }
        val existing = if (index != -1) currentOrders[index] else {
            return Result.failure(IllegalArgumentException("შეკვეთა ვერ მოიძებნა"))
        }
        val activeUid = activeRegisteredUidOrNull(context).orEmpty()
        if (existing.userId.isNotBlank() && activeUid.isNotBlank() &&
            existing.userId != activeUid && !isCurrentUserVerifiedSoleAdmin(context)
        ) {
            return Result.failure(SecurityException("PERMISSION_DENIED: სხვა მომხმარებლის შეკვეთაზე წვდომა აკრძალულია"))
        }
        if (existing.paymentMethodCode != PaymentMethodType.BANK_TRANSFER.code) {
            return Result.failure(IllegalStateException("ეს მოქმედება ხელმისაწვდომია მხოლოდ საბანკო გადარიცხვის შეკვეთებზე"))
        }
        if (existing.status == "გაუქმებული" || existing.paymentStatus == PaymentStatus.PAID.code || existing.paymentStatus == PaymentStatus.CANCELLED.code) {
            return Result.failure(IllegalStateException("შეკვეთა უკვე დახურულია ან დადასტურებულია"))
        }
        if (existing.paymentStatus == PaymentStatus.VERIFICATION_PENDING.code) {
            // Idempotent guard: prevent duplicate admin notifications on double tap
            return Result.success(existing)
        }

        val now = System.currentTimeMillis()
        val validBank = bankOverride?.takeIf { it.isEnabled && BstorePaymentManager.isValidIban(it.iban) }
        val updated = existing.copy(
            paymentStatus = PaymentStatus.VERIFICATION_PENDING.code,
            userConfirmedPaymentAtMillis = now,
            bankAccountId = validBank?.id ?: existing.bankAccountId,
            bankCode = validBank?.bankCode ?: existing.bankCode,
            bankName = validBank?.bankName ?: existing.bankName,
            recipientNameSnapshot = validBank?.recipientName ?: existing.recipientNameSnapshot,
            ibanSnapshot = validBank?.let { BstorePaymentManager.normalizeIban(it.iban) } ?: existing.ibanSnapshot,
            paymentMethod = validBank?.let { "საბანკო გადარიცხვა (${it.bankName})" } ?: existing.paymentMethod,
            paymentInstructionsSnapshot = validBank?.paymentInstructions?.ifBlank { BankAccountConfig.DEFAULT_PAYMENT_INSTRUCTIONS_KA }
                ?: existing.paymentInstructionsSnapshot
        )
        currentOrders[index] = updated
        saveOrders(context, currentOrders)
        BekaNotificationManager.recordTransferSentForAdminOnly(context, updated)
        return Result.success(updated)
    }

    /**
     * Strictly admin-only payment confirmation (Bank Transfer verification or COD collection).
     * Non-admin callers are rejected with a SecurityException.
     */
    fun confirmOrderPaymentLocalAsAdmin(
        context: Context,
        order: OrderRecord,
        internalNote: String = ""
    ): Result<OrderRecord> {
        if (!isCurrentUserVerifiedSoleAdmin(context)) {
            return Result.failure(
                SecurityException("PERMISSION_DENIED: გადახდის დადასტურება შეუძლია მხოლოდ ავტორიზებულ ადმინისტრატორს (beqa1994@gmail.com)")
            )
        }
        val methodType = PaymentMethodType.fromCode(order.paymentMethodCode, order.paymentMethod)
        val prevPaymentStatus = order.paymentStatus
        val nowMillis = System.currentTimeMillis()
        val adminUid = safeFirebaseUser()?.uid ?: getCurrentUser(context)?.id ?: "sole_admin_uid"

        val updatedOrder = order.copy(
            paymentStatus = PaymentStatus.PAID.code,
            paymentConfirmedAtMillis = nowMillis,
            paymentConfirmedBy = adminUid,
            paymentInternalNote = internalNote.trim()
        )

        val currentOrders = getOrders(context).toMutableList()
        val index = currentOrders.indexOfFirst { it.id == order.id }
        if (index != -1) {
            currentOrders[index] = updatedOrder
            saveOrders(context, currentOrders)
        }

        val numericAmount = order.totalAmount.filter { it.isDigit() || it == '.' }.toDoubleOrNull() ?: 0.0
        val eventType = if (methodType == PaymentMethodType.COD) {
            "COD_COLLECTION_CONFIRMED"
        } else {
            "BANK_TRANSFER_CONFIRMED"
        }

        BstorePaymentManager.recordAuditEventLocal(
            context = context,
            event = PaymentAuditEvent(
                id = "audit_${nowMillis}_${order.id.takeLast(4)}",
                orderId = order.id,
                orderNumber = order.orderNumber,
                eventType = eventType,
                previousStatus = prevPaymentStatus,
                newStatus = PaymentStatus.PAID.code,
                amount = numericAmount,
                actorUid = adminUid,
                actorEmail = SOLE_ADMIN_EMAIL,
                actorRole = "sole_admin",
                timestampMillis = nowMillis,
                note = internalNote.trim().ifBlank {
                    if (methodType == PaymentMethodType.COD) "ნაღდი ანგარიშსწორება მიღებულია"
                    else "საბანკო ჩარიცხვა დადასტურებულია (${order.bankName})"
                }
            )
        )

        return Result.success(updatedOrder)
    }

    fun updateOrderPaymentStatusLocalAsAdmin(
        context: Context,
        order: OrderRecord,
        newPaymentStatus: PaymentStatus,
        internalNote: String = ""
    ): Result<OrderRecord> {
        if (!isCurrentUserVerifiedSoleAdmin(context)) {
            return Result.failure(
                SecurityException("PERMISSION_DENIED: გადახდის სტატუსის შეცვლა შეუძლია მხოლოდ ავტორიზებულ ადმინისტრატორს (beqa1994@gmail.com)")
            )
        }
        val nowMillis = System.currentTimeMillis()
        val adminUid = safeFirebaseUser()?.uid ?: getCurrentUser(context)?.id ?: "sole_admin_uid"
        val prevStatus = order.paymentStatus

        val updatedOrder = order.copy(
            paymentStatus = newPaymentStatus.code,
            paymentConfirmedAtMillis = if (newPaymentStatus == PaymentStatus.PAID) nowMillis else order.paymentConfirmedAtMillis,
            paymentConfirmedBy = if (newPaymentStatus == PaymentStatus.PAID) adminUid else order.paymentConfirmedBy,
            paymentInternalNote = internalNote.trim().ifBlank { order.paymentInternalNote }
        )

        val currentOrders = getOrders(context).toMutableList()
        val index = currentOrders.indexOfFirst { it.id == order.id }
        if (index != -1) {
            currentOrders[index] = updatedOrder
            saveOrders(context, currentOrders)
        }

        val numericAmount = order.totalAmount.filter { it.isDigit() || it == '.' }.toDoubleOrNull() ?: 0.0
        BstorePaymentManager.recordAuditEventLocal(
            context = context,
            event = PaymentAuditEvent(
                id = "audit_${nowMillis}_${order.id.takeLast(4)}",
                orderId = order.id,
                orderNumber = order.orderNumber,
                eventType = "PAYMENT_STATUS_CHANGED",
                previousStatus = prevStatus,
                newStatus = newPaymentStatus.code,
                amount = numericAmount,
                actorUid = adminUid,
                actorEmail = SOLE_ADMIN_EMAIL,
                actorRole = "sole_admin",
                timestampMillis = nowMillis,
                note = internalNote.trim()
            )
        )

        return Result.success(updatedOrder)
    }

    fun deleteOrder(context: Context, orderId: String): Boolean {
        val currentOrders = getOrders(context).toMutableList()
        val removed = currentOrders.removeAll { it.id == orderId }
        if (removed) {
            saveOrders(context, currentOrders)
            return true
        }
        return false
    }

    // =========================================================================
    // ფავორიტების მართვა ანგარიშის (Account / UID) დონეზე + Cloud სინქრონიზაცია
    // =========================================================================
    private fun resolveActiveFavoritesStorageKey(context: Context): String {
        val uid = activeRegisteredUidOrNull(context)
        return uid?.takeIf { it.isNotBlank() }?.let { userKey(KEY_FAVORITES_JSON, it) }
            ?: KEY_GUEST_FAVORITES_JSON
    }

    private fun readFavoritesSetFromKey(prefs: SharedPreferences, key: String): Set<Int> {
        val raw = prefs.getString(key, null) ?: return emptySet()
        if (raw.isBlank()) return emptySet()
        return try {
            val arr = JSONArray(raw)
            val set = mutableSetOf<Int>()
            for (i in 0 until arr.length()) {
                val id = arr.optInt(i, -1)
                if (id > 0) set.add(id)
            }
            set
        } catch (_: Exception) {
            emptySet()
        }
    }

    private fun writeFavoritesSetToKey(prefs: SharedPreferences, key: String, ids: Set<Int>) {
        val arr = JSONArray()
        ids.filter { it > 0 }.sorted().forEach { arr.put(it) }
        prefs.edit().putString(key, arr.toString()).commit()
    }

    fun getFavoriteProductIds(context: Context): Set<Int> {
        val prefs = getPrefs(context)
        val storageKey = resolveActiveFavoritesStorageKey(context)
        return readFavoritesSetFromKey(prefs, storageKey)
    }

    fun isProductFavorite(context: Context, productId: Int): Boolean {
        return productId in getFavoriteProductIds(context)
    }

    fun toggleFavorite(context: Context, productId: Int): Boolean {
        val prefs = getPrefs(context)
        val storageKey = resolveActiveFavoritesStorageKey(context)
        val currentFavs = readFavoritesSetFromKey(prefs, storageKey).toMutableSet()
        val isNowFavorite = if (productId in currentFavs) {
            currentFavs.remove(productId)
            false
        } else {
            currentFavs.add(productId)
            true
        }
        writeFavoritesSetToKey(prefs, storageKey, currentFavs)
        favoritesStateVersion.intValue++

        val fbUser = safeFirebaseUser()?.takeIf { !it.isAnonymous }
        if (fbUser != null) {
            val snapshotSet = currentFavs.toSet()
            val fallbackUser = getCurrentUser(context)
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    FirestoreRepository(context).saveUserFavoritesWithResult(snapshotSet, fallbackUser)
                } catch (e: Exception) {
                    Log.w("AuthAndOrderManager", "Could not sync favorites to Firestore: ${e.message}")
                }
            }
        }
        return isNowFavorite
    }

    fun setFavoriteProductIdsFromCloud(context: Context, cloudIds: Set<Int>) {
        val prefs = getPrefs(context)
        val uid = activeRegisteredUidOrNull(context)
        if (uid.isNullOrBlank()) return
        val key = userKey(KEY_FAVORITES_JSON, uid)
        val normalized = cloudIds.filter { it > 0 }.toSet()
        val existing = readFavoritesSetFromKey(prefs, key)
        if (existing != normalized) {
            writeFavoritesSetToKey(prefs, key, normalized)
            favoritesStateVersion.intValue++
        }
    }
}
