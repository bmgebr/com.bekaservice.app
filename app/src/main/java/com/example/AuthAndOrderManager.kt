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
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
            return if (combined.isNotBlank()) combined else if (name.isNotBlank()) name else email.substringBefore("@")
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
    val paymentMethod: String = "ნაღდი / საბანკო გადარიცხვა",
    val trackingCode: String
)

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

    private fun getPrefs(context: Context): SharedPreferences {
        return context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private tailrec fun Context.findActivity(): android.app.Activity? = when (this) {
        is android.app.Activity -> this
        is android.content.ContextWrapper -> baseContext.findActivity()
        else -> null
    }

    // =========================================================================
    // FIREBASE AUTH + JETPACK CREDENTIAL MANAGER (GOOGLE SIGN-IN)
    // =========================================================================
    @Volatile
    private var hasAttemptedAutoCredentialInSession = false

    suspend fun attemptAutoSignIn(context: Context): UserProfile? {
        val firebaseUser = safeFirebaseUser()
        if (firebaseUser != null) {
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
        return syncFirebaseUserToLocal(context, firebaseUser)
    }

    private suspend fun syncFirebaseUserToLocal(
        context: Context,
        firebaseUser: com.google.firebase.auth.FirebaseUser
    ): UserProfile {
        val uid = firebaseUser.uid
        val prefs = getPrefs(context)
        val email = firebaseUser.email
            ?: prefs.getString(userKey(KEY_USER_EMAIL, uid), null)
            ?: ""
        val fullName = firebaseUser.displayName?.takeIf { it.isNotBlank() }
            ?: prefs.getString(userKey(KEY_USER_NAME, uid), null)?.takeIf { it.isNotBlank() }
            ?: email.substringBefore("@").ifBlank { "მომხმარებელი" }
        val first = fullName.split(" ").firstOrNull() ?: fullName
        val last = fullName.split(" ").drop(1).joinToString(" ")
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
        val resolvedDisplayName = cloudProfile?.displayName?.takeIf { it.isNotBlank() } ?: fullName
        val resolvedAvatar = cloudProfile?.photoUrl?.takeIf { it.isNotBlank() } ?: photo

        val user = UserProfile(
            id = uid,
            name = resolvedDisplayName,
            firstName = resolvedDisplayName.split(" ").firstOrNull() ?: first,
            lastName = resolvedDisplayName.split(" ").drop(1).joinToString(" ").ifBlank { last },
            email = email,
            avatarUrl = resolvedAvatar,
            provider = "Google",
            phone = resolvedPhone,
            address = resolvedAddress,
            deviceModel = resolvedModel
        )

        // Update local session cache ONLY — never overwrite an existing Firestore user document on login!
        saveLocalSessionOnly(context, user)

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
    private fun safeFirebaseUser() = runCatching { FirebaseAuth.getInstance().currentUser }.getOrNull()

    fun getCurrentUser(context: Context): UserProfile? {
        val prefs = getPrefs(context)
        val firebaseUser = safeFirebaseUser()
        val isLocalLoggedIn = prefs.getBoolean(KEY_IS_LOGGED_IN, false)

        if (firebaseUser == null && !isLocalLoggedIn) return null

        val uid = firebaseUser?.uid ?: prefs.getString(KEY_USER_ID, null) ?: return null
        val isSameCachedUid = prefs.getString(KEY_USER_ID, null) == uid

        val resolvedEmail = firebaseUser?.email
            ?: prefs.getString(userKey(KEY_USER_EMAIL, uid), null)
            ?: (if (isSameCachedUid) prefs.getString(KEY_USER_EMAIL, null) else null)
            ?: ""

        val storedName = prefs.getString(userKey(KEY_USER_NAME, uid), null)?.takeIf { it.isNotBlank() }
            ?: (if (isSameCachedUid) prefs.getString(KEY_USER_NAME, null)?.takeIf { it.isNotBlank() } else null)
            ?: firebaseUser?.displayName?.takeIf { it.isNotBlank() }
            ?: resolvedEmail.substringBefore("@").ifBlank { "მომხმარებელი" }
        val first = prefs.getString(userKey(KEY_USER_FIRST_NAME, uid), null)
            ?: (if (isSameCachedUid) prefs.getString(KEY_USER_FIRST_NAME, "") else "")
            ?: ""
        val last = prefs.getString(userKey(KEY_USER_LAST_NAME, uid), null)
            ?: (if (isSameCachedUid) prefs.getString(KEY_USER_LAST_NAME, "") else "")
            ?: ""
        val resolvedFirst = if (first.isNotBlank()) first else storedName.split(" ").firstOrNull() ?: storedName
        val resolvedLast = if (last.isNotBlank()) last else storedName.split(" ").drop(1).joinToString(" ")

        val rawPhone = prefs.getString(userKey(KEY_USER_PHONE, uid), null)
            ?: (if (isSameCachedUid) prefs.getString(KEY_USER_PHONE, null) else null)
        val rawAddress = prefs.getString(userKey(KEY_USER_ADDRESS, uid), null)
            ?: (if (isSameCachedUid) prefs.getString(KEY_USER_ADDRESS, null) else null)
        val resolvedPhone = sanitizeUserPhone(rawPhone)
        val resolvedAddress = sanitizeUserAddress(rawAddress)
        val resolvedDeviceModel = prefs.getString(userKey(KEY_USER_DEVICE_MODEL, uid), null)
            ?: (if (isSameCachedUid) prefs.getString(KEY_USER_DEVICE_MODEL, null) else null)
            ?: ""

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
            provider = if (firebaseUser != null) "Google" else (prefs.getString(KEY_USER_PROVIDER, "Email") ?: "Email"),
            phone = resolvedPhone,
            address = resolvedAddress,
            deviceModel = resolvedDeviceModel
        )
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
        deviceModel: String = ""
    ): UserProfile {
        val prefs = getPrefs(context)
        val firebaseUid = safeFirebaseUser()?.uid ?: ("google_" + email.lowercase().hashCode())
        val resolvedModel = deviceModel.ifBlank {
            prefs.getString(userKey(KEY_USER_DEVICE_MODEL, firebaseUid), "") ?: ""
        }
        val cleanName = name.ifBlank { email.substringBefore("@") }
        val user = UserProfile(
            id = firebaseUid,
            name = cleanName,
            firstName = firstName.ifBlank { cleanName.split(" ").firstOrNull() ?: cleanName },
            lastName = lastName.ifBlank { cleanName.split(" ").drop(1).joinToString(" ") },
            email = email,
            avatarUrl = avatarUrl,
            provider = "Google",
            phone = sanitizeUserPhone(phone.ifBlank { prefs.getString(userKey(KEY_USER_PHONE, firebaseUid), "") }),
            address = sanitizeUserAddress(address.ifBlank { prefs.getString(userKey(KEY_USER_ADDRESS, firebaseUid), "") }),
            deviceModel = resolvedModel
        )
        saveLocalSessionOnly(context, user)
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
        val appleUid = "apple_" + email.lowercase().hashCode()
        val resolvedModel = deviceModel.ifBlank {
            prefs.getString(userKey(KEY_USER_DEVICE_MODEL, appleUid), "") ?: ""
        }
        val cleanName = name.ifBlank { email.substringBefore("@") }
        val user = UserProfile(
            id = appleUid,
            name = cleanName,
            firstName = firstName.ifBlank { cleanName.split(" ").firstOrNull() ?: cleanName },
            lastName = lastName.ifBlank { cleanName.split(" ").drop(1).joinToString(" ") },
            email = email,
            avatarUrl = "",
            provider = "Apple",
            phone = sanitizeUserPhone(phone.ifBlank { prefs.getString(userKey(KEY_USER_PHONE, appleUid), "") }),
            address = sanitizeUserAddress(address.ifBlank { prefs.getString(userKey(KEY_USER_ADDRESS, appleUid), "") }),
            deviceModel = resolvedModel
        )
        saveLocalSessionOnly(context, user)
        return user
    }

    fun loginWithEmail(context: Context, email: String, pass: String): Pair<Boolean, String> {
        if (email.isBlank() || !email.contains("@")) {
            return Pair(false, "გთხოვთ შეიყვანოთ სწორი ელ. ფოსტა")
        }
        if (pass.length < 6) {
            return Pair(false, "პაროლი უნდა შედგებოდეს მინიმუმ 6 სიმბოლოსგან")
        }

        val prefs = getPrefs(context)
        val uid = safeFirebaseUser()?.uid ?: ("email_" + email.trim().lowercase().hashCode())
        val savedNameForUid = prefs.getString(userKey(KEY_USER_FIRST_NAME, uid), null)

        val firstName = if (!savedNameForUid.isNullOrBlank()) {
            savedNameForUid
        } else {
            email.substringBefore("@").replaceFirstChar { it.uppercase() }
        }
        val lastName = prefs.getString(userKey(KEY_USER_LAST_NAME, uid), "") ?: ""
        val fullName = if (lastName.isNotBlank()) "$firstName $lastName" else firstName

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
        if (prefs.getString(KEY_USER_PASSWORD, null) == null) {
            prefs.edit().putString(KEY_USER_PASSWORD, pass).apply()
        }
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

        val rFirst = if (firstName.isNotBlank()) firstName else name.split(" ").firstOrNull() ?: name
        val rLast = if (lastName.isNotBlank()) lastName else name.split(" ").drop(1).joinToString(" ")
        val fullName = "$rFirst $rLast".trim()
        val uid = safeFirebaseUser()?.uid ?: ("email_" + email.trim().lowercase().hashCode())

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
        getPrefs(context).edit().putString(KEY_USER_PASSWORD, pass).apply()
        return Pair(true, "რეგისტრაცია წარმატებით დასრულდა!")
    }

    fun updateUserProfile(context: Context, updated: UserProfile): UserProfile {
        saveUserSession(context, updated)
        return updated
    }

    suspend fun saveUserProfileToCloud(context: Context, updated: UserProfile): Result<UserProfile> {
        val fbUser = safeFirebaseUser()
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

    fun changePassword(context: Context, oldPass: String, newPass: String): Pair<Boolean, String> {
        val prefs = getPrefs(context)
        val currentPass = prefs.getString(KEY_USER_PASSWORD, "password123") ?: "password123"

        if (oldPass != currentPass && currentPass.isNotEmpty()) {
            return Pair(false, "მიმდინარე პაროლი არასწორია")
        }
        if (newPass.length < 6) {
            return Pair(false, "ახალი პაროლი უნდა შეიცავდეს მინიმუმ 6 სიმბოლოს")
        }

        prefs.edit().putString(KEY_USER_PASSWORD, newPass).apply()
        return Pair(true, "პაროლი წარმატებით შეიცვალა!")
    }

    fun logout(context: Context) {
        try {
            FirebaseAuth.getInstance().signOut()
        } catch (_: Exception) {}
        CoroutineScope(Dispatchers.IO).launch {
            try {
                CredentialManager.create(context.applicationContext)
                    .clearCredentialState(ClearCredentialStateRequest())
            } catch (_: Exception) {}
        }
        getPrefs(context).edit()
            .putBoolean(KEY_IS_LOGGED_IN, false)
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
        favoritesStateVersion.intValue++
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
        val uid = safeFirebaseUser()?.uid
            ?: if (getPrefs(context).getBoolean(KEY_IS_LOGGED_IN, false)) {
                getPrefs(context).getString(KEY_USER_ID, null)
            } else {
                null
            }
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

                list.add(
                    OrderRecord(
                        id = orderId,
                        orderNumber = obj.getString("orderNumber"),
                        date = obj.getString("date"),
                        items = itemsList,
                        totalAmount = obj.getString("totalAmount"),
                        status = obj.getString("status"),
                        paymentMethod = obj.optString("paymentMethod", "ნაღდი / საბანკო გადარიცხვა"),
                        trackingCode = obj.optString("trackingCode", "BS-TRK-10000")
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

    fun buildOrderRecordFromCart(context: Context, cartItems: List<CartItem>): OrderRecord {
        val currentOrders = getOrders(context)
        val num = (1053 + currentOrders.size)
        val now = SimpleDateFormat("dd MMMM, yyyy • HH:mm", Locale.forLanguageTag("ka")).format(Date())

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
        return OrderRecord(
            id = "ord_${System.currentTimeMillis()}",
            orderNumber = "#BS-$num",
            date = now,
            items = details,
            totalAmount = totalFormatted,
            status = "მიმდინარე",
            paymentMethod = "WhatsApp & ნაღდი / საბანკო გადარიცხვა",
            trackingCode = "BS-TRK-${(10000..99999).random()}"
        )
    }

    suspend fun createAndPersistOrderFromCart(
        context: Context,
        cartItems: List<CartItem>
    ): Result<OrderRecord> {
        val newOrder = buildOrderRecordFromCart(context, cartItems)
        val fbUser = safeFirebaseUser()
        return if (fbUser != null) {
            val repo = FirestoreRepository(context)
            val user = getCurrentUser(context)
            val res = repo.createOrderFromCart(newOrder, user)
            if (res.isSuccess) {
                val currentOrders = getOrders(context).toMutableList()
                currentOrders.removeAll { it.id == newOrder.id }
                currentOrders.add(0, newOrder)
                saveOrders(context, currentOrders)
                Result.success(newOrder)
            } else {
                Result.failure(
                    res.exceptionOrNull() ?: IllegalStateException("შეკვეთის შენახვა ვერ მოხერხდა")
                )
            }
        } else {
            val currentOrders = getOrders(context).toMutableList()
            currentOrders.add(0, newOrder)
            saveOrders(context, currentOrders)
            Result.success(newOrder)
        }
    }

    fun addOrderFromCart(context: Context, cartItems: List<CartItem>): OrderRecord {
        val newOrder = buildOrderRecordFromCart(context, cartItems)
        val currentOrders = getOrders(context).toMutableList()
        currentOrders.add(0, newOrder)
        saveOrders(context, currentOrders)

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

    fun updateOrderStatus(context: Context, orderId: String, newStatus: String): Boolean {
        val currentOrders = getOrders(context).toMutableList()
        val index = currentOrders.indexOfFirst { it.id == orderId }
        if (index != -1) {
            currentOrders[index] = currentOrders[index].copy(status = newStatus)
            saveOrders(context, currentOrders)
            return true
        }
        return false
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
        val prefs = getPrefs(context)
        val uid = safeFirebaseUser()?.uid
            ?: if (prefs.getBoolean(KEY_IS_LOGGED_IN, false)) {
                prefs.getString(KEY_USER_ID, null)
            } else {
                null
            }
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

        val fbUser = safeFirebaseUser()
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
        val uid = safeFirebaseUser()?.uid
            ?: if (prefs.getBoolean(KEY_IS_LOGGED_IN, false)) {
                prefs.getString(KEY_USER_ID, null)
            } else {
                null
            }
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
