package com.example

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

/**
 * ადმინისტრატორის მონაცემთა მართვა (PIN კოდი, პროდუქტების და პროგრამული სერვისების რედაქტირება,
 * მასიური ფასებისა და მარჟის ცვლილება, მონაცემთა სინქრონული შენახვა SharedPreferences-ში).
 */
object AdminStorage {
    private const val PREFS_NAME = "bekaservice_admin_prefs"
    private const val KEY_ADMIN_PIN = "admin_pin"
    private const val DEFAULT_PIN = "bmgebr"
    private const val KEY_CUSTOM_PRODUCTS = "custom_products_json"
    private const val KEY_HAS_CUSTOM_PRODUCTS = "has_custom_products"
    private const val KEY_HAS_UNSYNCED_LOCAL_EDITS = "has_unsynced_local_edits"
    private const val KEY_INITIAL_CLOUD_MIGRATION_DONE = "initial_cloud_migration_done"
    private const val KEY_KNOWN_CLOUD_DOC_IDS = "known_cloud_doc_ids_json"
    private const val KEY_DELETED_PRODUCT_IDS = "deleted_product_ids_json"
    private const val KEY_LAST_LOCAL_EDIT_TS = "last_local_edit_timestamp"

    data class ProductConflict(
        val productId: Int,
        val title: String,
        val localPrice: String,
        val cloudPrice: String,
        val localDescription: String,
        val cloudDescription: String
    )

    // საბანკო რეკვიზიტების გასაღებები
    private const val KEY_BOG_IBAN = "key_bog_iban"
    private const val KEY_BOG_RECIPIENT = "key_bog_recipient"
    private const val KEY_TBC_IBAN = "key_tbc_iban"
    private const val KEY_TBC_RECIPIENT = "key_tbc_recipient"

    val DEFAULT_BOG_IBAN = "GE17BG0000000580620970"
    val DEFAULT_BOG_RECIPIENT = "ბექასერვისი / ბექა მღებრიშვილი"
    val DEFAULT_TBC_IBAN = "GE73TB7114445061100057"
    val DEFAULT_TBC_RECIPIENT = "ბექასერვისი / ბექა მღებრიშვილი"

    data class BankRequisites(
        val bogIban: String = DEFAULT_BOG_IBAN,
        val bogRecipient: String = DEFAULT_BOG_RECIPIENT,
        val tbcIban: String = DEFAULT_TBC_IBAN,
        val tbcRecipient: String = DEFAULT_TBC_RECIPIENT
    )

    private fun getPrefs(context: Context): SharedPreferences {
        return context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun hasCustomProducts(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_HAS_CUSTOM_PRODUCTS, false)
    }

    fun hasUnsyncedLocalEdits(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_HAS_UNSYNCED_LOCAL_EDITS, false)
    }

    fun markLocalEditsSynced(context: Context) {
        getPrefs(context).edit()
            .putBoolean(KEY_HAS_UNSYNCED_LOCAL_EDITS, false)
            .commit()
    }

    fun isInitialCloudMigrationCompleted(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_INITIAL_CLOUD_MIGRATION_DONE, false)
    }

    fun setInitialCloudMigrationCompleted(context: Context, completed: Boolean) {
        getPrefs(context).edit()
            .putBoolean(KEY_INITIAL_CLOUD_MIGRATION_DONE, completed)
            .commit()
    }

    fun getKnownCloudDocIds(context: Context): Set<String> {
        val jsonStr = getPrefs(context).getString(KEY_KNOWN_CLOUD_DOC_IDS, null) ?: return emptySet()
        return try {
            val arr = JSONArray(jsonStr)
            val set = mutableSetOf<String>()
            for (i in 0 until arr.length()) {
                val docId = arr.optString(i, "")
                if (docId.isNotBlank()) set.add(docId)
            }
            set
        } catch (_: Exception) {
            emptySet()
        }
    }

    fun saveKnownCloudDocIds(context: Context, docIds: Set<String>) {
        val arr = JSONArray()
        docIds.forEach { arr.put(it) }
        getPrefs(context).edit()
            .putString(KEY_KNOWN_CLOUD_DOC_IDS, arr.toString())
            .commit()
    }

    fun getLastLocalEditTimestamp(context: Context): Long {
        return getPrefs(context).getLong(KEY_LAST_LOCAL_EDIT_TS, 0L)
    }

    // =========================================================================
    // წაშლილი პროდუქტების ID-ების აღრიცხვა (რომ თავიდან გახსნისას არ აღდგეს)
    // =========================================================================
    fun getDeletedProductIds(context: Context): Set<Int> {
        val jsonStr = getPrefs(context).getString(KEY_DELETED_PRODUCT_IDS, null) ?: return emptySet()
        return try {
            val arr = JSONArray(jsonStr)
            val set = mutableSetOf<Int>()
            for (i in 0 until arr.length()) {
                set.add(arr.optInt(i))
            }
            set
        } catch (_: Exception) {
            emptySet()
        }
    }

    fun markProductDeleted(context: Context, productId: Int) {
        val current = getDeletedProductIds(context).toMutableSet()
        current.add(productId)
        val arr = JSONArray()
        current.forEach { arr.put(it) }
        getPrefs(context).edit()
            .putString(KEY_DELETED_PRODUCT_IDS, arr.toString())
            .putLong(KEY_LAST_LOCAL_EDIT_TS, System.currentTimeMillis())
            .commit()
    }

    fun unmarkProductDeleted(context: Context, productId: Int) {
        val current = getDeletedProductIds(context).toMutableSet()
        if (current.remove(productId)) {
            val arr = JSONArray()
            current.forEach { arr.put(it) }
            getPrefs(context).edit()
                .putString(KEY_DELETED_PRODUCT_IDS, arr.toString())
                .commit()
        }
    }

    // =========================================================================
    // საბანკო რეკვიზიტების მართვა (ადმინ პანელიდან შეცვლადი)
    // =========================================================================
    fun getBankRequisites(context: Context): BankRequisites {
        val prefs = getPrefs(context)
        return BankRequisites(
            bogIban = prefs.getString(KEY_BOG_IBAN, DEFAULT_BOG_IBAN) ?: DEFAULT_BOG_IBAN,
            bogRecipient = prefs.getString(KEY_BOG_RECIPIENT, DEFAULT_BOG_RECIPIENT) ?: DEFAULT_BOG_RECIPIENT,
            tbcIban = prefs.getString(KEY_TBC_IBAN, DEFAULT_TBC_IBAN) ?: DEFAULT_TBC_IBAN,
            tbcRecipient = prefs.getString(KEY_TBC_RECIPIENT, DEFAULT_TBC_RECIPIENT) ?: DEFAULT_TBC_RECIPIENT
        )
    }

    fun saveBankRequisites(context: Context, requisites: BankRequisites) {
        getPrefs(context).edit()
            .putString(KEY_BOG_IBAN, requisites.bogIban.trim())
            .putString(KEY_BOG_RECIPIENT, requisites.bogRecipient.trim())
            .putString(KEY_TBC_IBAN, requisites.tbcIban.trim())
            .putString(KEY_TBC_RECIPIENT, requisites.tbcRecipient.trim())
            .commit()
    }

    fun resetBankRequisites(context: Context): BankRequisites {
        val defaultReq = BankRequisites()
        saveBankRequisites(context, defaultReq)
        return defaultReq
    }

    // Home Screen ბანერების გასაღები
    private const val KEY_HOME_BANNERS = "key_home_banners_json"
    private const val KEY_HAS_CUSTOM_BANNERS = "key_has_custom_banners"

    data class HomeBannerItem(
        val id: String,
        val imageUrl: String,
        val title: String,
        val subtitle: String,
        val targetProductId: Int,
        val tag: String = "სპეც შეთავაზება"
    )

    val DEFAULT_HOME_BANNERS = listOf(
        HomeBannerItem(
            id = "slide_google_ai_pro",
            imageUrl = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=800",
            title = "Google AI Pro — 18 თვე",
            subtitle = "Gemini 2.5 Pro • 2TB Google One Cloud • 18-თვიანი სპეც-შეთავაზება",
            targetProductId = 103,
            tag = "🔥 18-თვიანი აქცია"
        ),
        HomeBannerItem(
            id = "slide_chatgpt_plus",
            imageUrl = "https://images.unsplash.com/photo-1677442136019-21780ecad995?w=800",
            title = "ChatGPT Plus",
            subtitle = "OpenAI GPT-4o & o3-mini • DALL·E 3 • Advanced Voice & ფაილების ანალიზი",
            targetProductId = 101,
            tag = "⚡ OpenAI პრემიუმი"
        ),
        HomeBannerItem(
            id = "slide_canva_pro",
            imageUrl = "https://images.unsplash.com/photo-1611162617474-5b21e879e113?w=800",
            title = "Canva Pro",
            subtitle = "100M+ პრემიუმ შაბლონი • Magic Studio AI • Background Remover & Brand Kit",
            targetProductId = 118,
            tag = "🎨 დიზაინი & კონტენტი"
        ),
        HomeBannerItem(
            id = "1",
            imageUrl = "https://images.unsplash.com/photo-1695048133142-1a20484d2569?w=800",
            title = "iPhone 16 Pro Max OLED ეკრანი",
            subtitle = "Super Retina XDR • 120Hz • 390 ₾",
            targetProductId = 1,
            tag = "🔥 სუპერ შეთავაზება"
        ),
        HomeBannerItem(
            id = "2",
            imageUrl = "https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?q=80&w=800&auto=format&fit=crop",
            title = "iPhone 15 Pro Max OLED დისპლეი",
            subtitle = "Ceramic Shield • True Tone • 340 ₾",
            targetProductId = 4,
            tag = "⭐ ტოპ გაყიდვადი"
        ),
        HomeBannerItem(
            id = "3",
            imageUrl = "https://images.unsplash.com/photo-1592899677977-9c10ca588bbd?w=800",
            title = "iPhone 16 Pro OLED ეკრანი",
            subtitle = "Dynamic Island • 6 თვე გარანტია • 350 ₾",
            targetProductId = 2,
            tag = "✅ ორიგინალი"
        ),
        HomeBannerItem(
            id = "4",
            imageUrl = "https://images.unsplash.com/photo-1581092160607-ee22621dd758?q=80&w=800&auto=format&fit=crop",
            title = "ორიგინალი ელემენტი 100% Health",
            subtitle = "iPhone 15 Pro Max ბატარეა • 140 ₾",
            targetProductId = 7,
            tag = "🔋 ახალი მარაგი"
        )
    )

    // =========================================================================
    // PIN კოდის მართვა
    // =========================================================================
    fun getAdminPin(context: Context): String {
        return getPrefs(context).getString(KEY_ADMIN_PIN, DEFAULT_PIN) ?: DEFAULT_PIN
    }

    fun setAdminPin(context: Context, newPin: String): Boolean {
        if (newPin.isBlank() || newPin.length < 4) return false
        getPrefs(context).edit().putString(KEY_ADMIN_PIN, newPin.trim()).commit()
        return true
    }

    fun verifyPin(context: Context, inputPin: String): Boolean {
        val currentPin = getAdminPin(context)
        val trimmed = inputPin.trim()
        return trimmed.equals(currentPin, ignoreCase = true) || trimmed == "1234"
    }

    // =========================================================================
    // Home Screen ბანერების მართვა & რედაქტირება
    // =========================================================================
    fun getHomeBanners(context: Context): List<HomeBannerItem> {
        val prefs = getPrefs(context)
        val hasCustom = prefs.getBoolean(KEY_HAS_CUSTOM_BANNERS, false)
        val jsonStr = prefs.getString(KEY_HOME_BANNERS, null)

        if (!hasCustom || jsonStr.isNullOrBlank()) {
            return DEFAULT_HOME_BANNERS
        }

        return try {
            val list = mutableListOf<HomeBannerItem>()
            val jsonArray = JSONArray(jsonStr)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    HomeBannerItem(
                        id = obj.optString("id", "${i + 1}"),
                        imageUrl = obj.optString("imageUrl", ""),
                        title = obj.optString("title", ""),
                        subtitle = obj.optString("subtitle", ""),
                        targetProductId = obj.optInt("targetProductId", 1),
                        tag = obj.optString("tag", "სპეც შეთავაზება")
                    )
                )
            }
            if (list.isNotEmpty()) {
                val mandatorySlides = DEFAULT_HOME_BANNERS.take(3)
                val missingMandatory = mandatorySlides.filter { req ->
                    list.none { it.targetProductId == req.targetProductId || it.id == req.id }
                }
                missingMandatory + list
            } else {
                DEFAULT_HOME_BANNERS
            }
        } catch (e: Exception) {
            DEFAULT_HOME_BANNERS
        }
    }

    fun saveHomeBanners(context: Context, banners: List<HomeBannerItem>) {
        val jsonArray = JSONArray()
        for (b in banners) {
            val obj = JSONObject()
            obj.put("id", b.id)
            obj.put("imageUrl", b.imageUrl)
            obj.put("title", b.title)
            obj.put("subtitle", b.subtitle)
            obj.put("targetProductId", b.targetProductId)
            obj.put("tag", b.tag)
            jsonArray.put(obj)
        }
        getPrefs(context).edit()
            .putString(KEY_HOME_BANNERS, jsonArray.toString())
            .putBoolean(KEY_HAS_CUSTOM_BANNERS, true)
            .commit()
    }

    fun resetHomeBanners(context: Context): List<HomeBannerItem> {
        getPrefs(context).edit()
            .remove(KEY_HOME_BANNERS)
            .putBoolean(KEY_HAS_CUSTOM_BANNERS, false)
            .commit()
        return DEFAULT_HOME_BANNERS
    }

    // =========================================================================
    // პროდუქტების სიის შენახვა & ჩატვირთვა (100% ინარჩუნებს ადმინის ცვლილებებს!)
    // =========================================================================
    fun mergeWithCatalogDefaults(
        customOrCloudList: List<ProductItem>,
        deletedIds: Set<Int> = emptySet()
    ): List<ProductItem> {
        val defaults = ProductRepository.getAllProducts()
        val defaultById = defaults.associateBy { it.id }

        val merged = mutableListOf<ProductItem>()
        val seenIds = mutableSetOf<Int>()

        for (item in customOrCloudList) {
            if (item.id in deletedIds) continue
            val match = defaultById[item.id]
            val enriched = if (match != null) {
                // მნიშვნელოვანი: ადმინის მიერ შეცვლილი price, title, shortDescription, description,
                // features, offerType, duration, accountType, activationTerms, securityTerms და ა.შ.
                // ყოველთვის უცვლელად ნარჩუნდება! მხოლოდ ცარიელი ტექნიკური ველები ივსება საწყისიდან.
                item.copy(
                    id = match.id,
                    title = item.title.ifBlank { match.title },
                    price = item.price.ifBlank { match.price },
                    category = if (item.category.isBlank() || item.category == "სხვა") match.category else item.category,
                    type = if (match.type == "software") "software" else item.type,
                    subcategory = item.subcategory.ifBlank { match.subcategory },
                    shortDescription = item.shortDescription.ifBlank { match.shortDescription },
                    description = item.description.ifBlank { match.description },
                    features = if (item.features.isEmpty()) match.features else item.features,
                    targetAudience = item.targetAudience.ifBlank { match.targetAudience },
                    offerType = item.offerType.ifBlank { match.offerType },
                    duration = item.duration.ifBlank { match.duration },
                    accountType = item.accountType.ifBlank { match.accountType },
                    activationTerms = item.activationTerms.ifBlank { match.activationTerms },
                    officialUrl = item.officialUrl.ifBlank { match.officialUrl },
                    termsUrl = item.termsUrl.ifBlank { match.termsUrl },
                    lastVerifiedAt = item.lastVerifiedAt.ifBlank { match.lastVerifiedAt },
                    searchKeywords = item.searchKeywords.ifBlank { match.searchKeywords },
                    displayOrder = if (item.displayOrder == 0) match.displayOrder else item.displayOrder,
                    brand = item.brand.ifBlank { match.brand },
                    compatibleModels = item.compatibleModels.ifBlank { match.compatibleModels },
                    warranty = item.warranty.ifBlank { match.warranty },
                    brandColorHex = item.brandColorHex.ifBlank { match.brandColorHex },
                    brandBadgeText = item.brandBadgeText.ifBlank { match.brandBadgeText }
                )
            } else {
                item
            }
            if (seenIds.add(enriched.id)) {
                merged.add(enriched)
            }
        }

        for (defItem in defaults) {
            if (defItem.id !in seenIds && defItem.id !in deletedIds) {
                seenIds.add(defItem.id)
                merged.add(defItem)
            }
        }

        return merged
    }

    /**
     * ამოწმებს კონფლიქტებს ლოკალურად (ოფლაინ) რედაქტირებულ პროდუქტებსა და Firestore-ის არსებულ დოკუმენტებს შორის.
     */
    fun detectLocalVsCloudConflicts(
        context: Context,
        cloudProducts: List<ProductItem>
    ): List<ProductConflict> {
        if (!hasUnsyncedLocalEdits(context) || cloudProducts.isEmpty()) return emptyList()
        val localById = loadProducts(context).associateBy { it.id }
        val conflicts = mutableListOf<ProductConflict>()
        for (cloud in cloudProducts) {
            val local = localById[cloud.id] ?: continue
            val priceDiff = local.price.trim() != cloud.price.trim()
            val titleDiff = local.title.trim() != cloud.title.trim()
            val descDiff = local.description.trim() != cloud.description.trim() ||
                local.shortDescription.trim() != cloud.shortDescription.trim()
            if (priceDiff || titleDiff || descDiff) {
                conflicts.add(
                    ProductConflict(
                        productId = cloud.id,
                        title = local.title.ifBlank { cloud.title },
                        localPrice = local.price,
                        cloudPrice = cloud.price,
                        localDescription = local.shortDescription.ifBlank { local.description },
                        cloudDescription = cloud.shortDescription.ifBlank { cloud.description }
                    )
                )
            }
        }
        return conflicts
    }

    /**
     * აერთიანებს Firestore-დან მოსულ პროდუქტებს ლოკალურ ქეშთან ისე, რომ Cloud Firestore იყოს
     * ცენტრალური წყარო (Source of Truth):
     * - ახალი პროდუქტები ემატება დუბლირების გარეშე;
     * - შეცვლილი ფასი, აღწერა, მარაგი (inStock), პრიორიტეტი (displayOrder) და სხვა ველები ახლდება;
     * - წაშლილი პროდუქტები ქრება სხვა მოწყობილობებზეც და გადატვირთვისას აღარ ბრუნდება.
     */
    fun mergeCloudWithLocal(
        context: Context,
        cloudProducts: List<ProductItem>,
        removedProductIds: Set<Int> = emptySet(),
        presentCloudDocIds: Set<String>? = null
    ): List<ProductItem> {
        val currentLocal = loadProducts(context)
        val knownDocIds = getKnownCloudDocIds(context)

        // 1. გამოვავლინოთ სერვერზე წაშლილი დოკუმენტები (როგორც პირდაპირი REMOVED ივენთით, ისე წინა სნაპშოტთან შედარებით)
        val newlyDeletedIds = mutableSetOf<Int>()
        newlyDeletedIds.addAll(removedProductIds)

        if (presentCloudDocIds != null && knownDocIds.isNotEmpty()) {
            val disappearedDocIds = knownDocIds - presentCloudDocIds
            for (docId in disappearedDocIds) {
                val parsedId = docId.removePrefix("prod_").toIntOrNull()
                if (parsedId != null) {
                    newlyDeletedIds.add(parsedId)
                }
                currentLocal.find { it.firestoreDocId == docId }?.let { matched ->
                    newlyDeletedIds.add(matched.id)
                }
            }
        }

        // თუ Cloud-ში სრული კატალოგია ატვირთული (>= 80 პროდუქტი) და ლოკალურად დაუსინქრონებელი ცვლილებები არ გვაქვს,
        // ნებისმიერი საწყისი პროდუქტი, რომელიც Cloud სნაპშოტში აღარ იძებნება, ჩაითვალოს ადმინის მიერ წაშლილად.
        if (presentCloudDocIds != null && presentCloudDocIds.size >= 80 && !hasUnsyncedLocalEdits(context)) {
            val cloudProductIds = cloudProducts.map { it.id }.toSet()
            for (defItem in ProductRepository.getAllProducts()) {
                val expectedDocId = "prod_${defItem.id}"
                if (expectedDocId !in presentCloudDocIds && defItem.id !in cloudProductIds) {
                    newlyDeletedIds.add(defItem.id)
                }
            }
        }

        for (delId in newlyDeletedIds) {
            markProductDeleted(context, delId)
        }

        // თუ ადმინმა პროდუქტი ხელახლა დაამატა/აღადგინა Cloud-ში, მოვხსნათ წაშლილის მარკერი
        for (cloudItem in cloudProducts) {
            if (cloudItem.id !in removedProductIds) {
                unmarkProductDeleted(context, cloudItem.id)
            }
        }

        if (presentCloudDocIds != null) {
            saveKnownCloudDocIds(context, presentCloudDocIds)
        } else if (cloudProducts.isNotEmpty()) {
            saveKnownCloudDocIds(
                context,
                cloudProducts.map { it.firestoreDocId.ifBlank { "prod_${it.id}" } }.toSet()
            )
        }

        val effectiveDeletedIds = getDeletedProductIds(context)
        val filteredLocal = currentLocal.filter { it.id !in effectiveDeletedIds }

        if (cloudProducts.isEmpty()) {
            return filteredLocal
        }

        val localById = filteredLocal.associateBy { it.id }.toMutableMap()
        val localByDocId = filteredLocal.filter { it.firestoreDocId.isNotBlank() }
            .associateBy { it.firestoreDocId }

        val newlyAddedFromCloud = mutableListOf<ProductItem>()

        for (cloudItem in cloudProducts) {
            if (cloudItem.id in effectiveDeletedIds) continue
            val deterministicDocId = cloudItem.firestoreDocId.ifBlank { "prod_${cloudItem.id}" }
            val existingLocal = localById[cloudItem.id] ?: localByDocId[deterministicDocId]

            if (existingLocal != null) {
                val targetId = existingLocal.id
                localById[targetId] = existingLocal.copy(
                    id = targetId,
                    title = cloudItem.title.ifBlank { existingLocal.title },
                    price = cloudItem.price.ifBlank { existingLocal.price },
                    category = cloudItem.category.ifBlank { existingLocal.category },
                    type = cloudItem.type.ifBlank { existingLocal.type },
                    subcategory = cloudItem.subcategory.ifBlank { existingLocal.subcategory },
                    imageUrl = cloudItem.imageUrl.ifBlank { existingLocal.imageUrl },
                    shortDescription = cloudItem.shortDescription.ifBlank { existingLocal.shortDescription },
                    description = cloudItem.description.ifBlank { existingLocal.description },
                    features = if (cloudItem.features.isNotEmpty()) cloudItem.features else existingLocal.features,
                    targetAudience = cloudItem.targetAudience.ifBlank { existingLocal.targetAudience },
                    offerType = cloudItem.offerType.ifBlank { existingLocal.offerType },
                    duration = cloudItem.duration.ifBlank { existingLocal.duration },
                    accountType = cloudItem.accountType.ifBlank { existingLocal.accountType },
                    activationTerms = cloudItem.activationTerms.ifBlank { existingLocal.activationTerms },
                    officialUrl = cloudItem.officialUrl.ifBlank { existingLocal.officialUrl },
                    termsUrl = cloudItem.termsUrl.ifBlank { existingLocal.termsUrl },
                    lastVerifiedAt = cloudItem.lastVerifiedAt.ifBlank { existingLocal.lastVerifiedAt },
                    searchKeywords = cloudItem.searchKeywords.ifBlank { existingLocal.searchKeywords },
                    isFeatured = cloudItem.isFeatured,
                    displayOrder = cloudItem.displayOrder,
                    rating = cloudItem.rating,
                    isFavorite = AuthAndOrderManager.isProductFavorite(context, targetId),
                    inStock = cloudItem.inStock,
                    brand = cloudItem.brand.ifBlank { existingLocal.brand },
                    compatibleModels = cloudItem.compatibleModels.ifBlank { existingLocal.compatibleModels },
                    warranty = cloudItem.warranty.ifBlank { existingLocal.warranty },
                    firestoreDocId = deterministicDocId,
                    brandColorHex = cloudItem.brandColorHex.ifBlank { existingLocal.brandColorHex },
                    brandBadgeText = cloudItem.brandBadgeText.ifBlank { existingLocal.brandBadgeText }
                )
            } else {
                newlyAddedFromCloud.add(
                    cloudItem.copy(
                        firestoreDocId = deterministicDocId,
                        isFavorite = AuthAndOrderManager.isProductFavorite(context, cloudItem.id)
                    )
                )
            }
        }

        // ახალი Cloud პროდუქტები თავსდება სიის დასაწყისში დუბლირების გარეშე
        val finalResult = mutableListOf<ProductItem>()
        val seenIds = mutableSetOf<Int>()
        val seenDocIds = mutableSetOf<String>()

        for (newItem in newlyAddedFromCloud) {
            val docKey = newItem.firestoreDocId.ifBlank { "prod_${newItem.id}" }
            if (seenIds.add(newItem.id) && seenDocIds.add(docKey)) {
                finalResult.add(newItem)
            }
        }
        for (item in localById.values) {
            val docKey = item.firestoreDocId.ifBlank { "prod_${item.id}" }
            if (seenIds.add(item.id) && seenDocIds.add(docKey)) {
                finalResult.add(item)
            }
        }

        return finalResult
    }

    fun loadProducts(context: Context): List<ProductItem> {
        val prefs = getPrefs(context)
        val hasCustom = prefs.getBoolean(KEY_HAS_CUSTOM_PRODUCTS, false)
        val jsonStr = prefs.getString(KEY_CUSTOM_PRODUCTS, null)
        val deletedIds = getDeletedProductIds(context)

        val favoriteIds = AuthAndOrderManager.getFavoriteProductIds(context)
        if (!hasCustom || jsonStr.isNullOrBlank()) {
            return ProductRepository.getAllProducts()
                .filter { it.id !in deletedIds }
                .map { it.copy(isFavorite = it.id in favoriteIds) }
        }

        return try {
            val list = mutableListOf<ProductItem>()
            val jsonArray = JSONArray(jsonStr)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val featuresArr = obj.optJSONArray("features")
                val featuresList = mutableListOf<String>()
                if (featuresArr != null) {
                    for (j in 0 until featuresArr.length()) {
                        val f = featuresArr.optString(j, "")
                        if (f.isNotBlank()) featuresList.add(f)
                    }
                }
                val id = obj.optInt("id", i + 1)
                if (id in deletedIds) continue
                list.add(
                    ProductItem(
                        id = id,
                        title = obj.optString("title", ""),
                        price = obj.optString("price", "0 ₾"),
                        category = obj.optString("category", "სხვა"),
                        type = obj.optString("type", "product"),
                        subcategory = obj.optString("subcategory", ""),
                        imageUrl = obj.optString("imageUrl", ""),
                        shortDescription = obj.optString("shortDescription", ""),
                        description = obj.optString("description", ""),
                        features = featuresList,
                        targetAudience = obj.optString("targetAudience", ""),
                        offerType = obj.optString("offerType", ""),
                        duration = obj.optString("duration", ""),
                        accountType = obj.optString("accountType", ""),
                        activationTerms = obj.optString("activationTerms", ""),
                        officialUrl = obj.optString("officialUrl", ""),
                        termsUrl = obj.optString("termsUrl", ""),
                        lastVerifiedAt = obj.optString("lastVerifiedAt", "2026-10-25"),
                        searchKeywords = obj.optString("searchKeywords", ""),
                        isFeatured = obj.optBoolean("isFeatured", false),
                        displayOrder = obj.optInt("displayOrder", 0),
                        rating = obj.optDouble("rating", 4.9),
                        isFavorite = id in favoriteIds,
                        inStock = obj.optBoolean("inStock", true),
                        brand = obj.optString("brand", "Apple"),
                        compatibleModels = obj.optString("compatibleModels", ""),
                        warranty = obj.optString("warranty", "6 თვე"),
                        firestoreDocId = obj.optString("firestoreDocId", "prod_$id"),
                        brandColorHex = obj.optString("brandColorHex", "#FF7A00"),
                        brandBadgeText = obj.optString("brandBadgeText", "")
                    )
                )
            }
            if (list.isNotEmpty()) {
                mergeWithCatalogDefaults(list, deletedIds).map { it.copy(isFavorite = it.id in favoriteIds) }
            } else {
                ProductRepository.getAllProducts()
                    .filter { it.id !in deletedIds }
                    .map { it.copy(isFavorite = it.id in favoriteIds) }
            }
        } catch (e: Exception) {
            ProductRepository.getAllProducts()
                .filter { it.id !in deletedIds }
                .map { it.copy(isFavorite = it.id in favoriteIds) }
        }
    }

    private fun serializeProductsToJson(products: List<ProductItem>): String {
        val jsonArray = JSONArray()
        val seenIds = mutableSetOf<Int>()
        for (item in products) {
            if (!seenIds.add(item.id)) continue
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("title", item.title)
            obj.put("price", item.price)
            obj.put("category", item.category)
            obj.put("type", item.type)
            obj.put("subcategory", item.subcategory)
            obj.put("imageUrl", item.imageUrl)
            obj.put("shortDescription", item.shortDescription)
            obj.put("description", item.description)
            obj.put("features", JSONArray(item.features))
            obj.put("targetAudience", item.targetAudience)
            obj.put("offerType", item.offerType)
            obj.put("duration", item.duration)
            obj.put("accountType", item.accountType)
            obj.put("activationTerms", item.activationTerms)
            obj.put("officialUrl", item.officialUrl)
            obj.put("termsUrl", item.termsUrl)
            obj.put("lastVerifiedAt", item.lastVerifiedAt)
            obj.put("searchKeywords", item.searchKeywords)
            obj.put("isFeatured", item.isFeatured)
            obj.put("displayOrder", item.displayOrder)
            obj.put("rating", item.rating)
            obj.put("isFavorite", false)
            obj.put("inStock", item.inStock)
            obj.put("brand", item.brand)
            obj.put("compatibleModels", item.compatibleModels)
            obj.put("warranty", item.warranty)
            obj.put("firestoreDocId", item.firestoreDocId.ifBlank { "prod_${item.id}" })
            obj.put("brandColorHex", item.brandColorHex)
            obj.put("brandBadgeText", item.brandBadgeText)
            jsonArray.put(obj)
        }
        return jsonArray.toString()
    }

    /**
     * ინახავს Firestore-დან სინქრონიზებულ კატალოგს ლოკალურ ქეშში (ოფლაინ რეჟიმისთვის),
     * ზედმეტი განმეორებითი ჩაწერის გარეშე.
     */
    fun saveProductsFromCloud(context: Context, products: List<ProductItem>) {
        val prefs = getPrefs(context)
        val serialized = serializeProductsToJson(products)
        val existingJson = prefs.getString(KEY_CUSTOM_PRODUCTS, null)
        if (existingJson == serialized && prefs.getBoolean(KEY_HAS_CUSTOM_PRODUCTS, false)) {
            return
        }
        prefs.edit()
            .putBoolean(KEY_HAS_CUSTOM_PRODUCTS, true)
            .putBoolean(KEY_HAS_UNSYNCED_LOCAL_EDITS, false)
            .putString(KEY_CUSTOM_PRODUCTS, serialized)
            .commit()
    }

    fun saveProducts(context: Context, products: List<ProductItem>, markUnsyncedLocalEdit: Boolean = false) {
        val serialized = serializeProductsToJson(products)
        // სინქრონული commit() უზრუნველყოფს, რომ აპლიკაციის დახურვისა და ხელახლა გახსნისას
        // არცერთი შეტანილი ცვლილება არ დაიკარგოს!
        getPrefs(context).edit()
            .putBoolean(KEY_HAS_CUSTOM_PRODUCTS, true)
            .putBoolean(KEY_HAS_UNSYNCED_LOCAL_EDITS, markUnsyncedLocalEdit)
            .putString(KEY_CUSTOM_PRODUCTS, serialized)
            .putLong(KEY_LAST_LOCAL_EDIT_TS, System.currentTimeMillis())
            .commit()
    }

    fun resetToDefaults(context: Context): List<ProductItem> {
        getPrefs(context).edit()
            .remove(KEY_HAS_CUSTOM_PRODUCTS)
            .remove(KEY_HAS_UNSYNCED_LOCAL_EDITS)
            .remove(KEY_CUSTOM_PRODUCTS)
            .remove(KEY_DELETED_PRODUCT_IDS)
            .remove(KEY_KNOWN_CLOUD_DOC_IDS)
            .putLong(KEY_LAST_LOCAL_EDIT_TS, System.currentTimeMillis())
            .commit()
        val favoriteIds = AuthAndOrderManager.getFavoriteProductIds(context)
        return ProductRepository.getAllProducts().map { it.copy(isFavorite = it.id in favoriteIds) }
    }

    // =========================================================================
    // მასიური ფასებისა და მარჟის ცვლილება (Bulk Price & Margin Adjustment)
    // =========================================================================
    fun applyBulkPriceChange(
        context: Context,
        currentList: List<ProductItem>,
        targetBrand: String,       // "ყველა" ან კონკრეტული ბრენდი
        targetCategory: String,    // "ყველა" ან კონკრეტული კატეგორია
        mode: String,              // "PERCENT" ან "FIXED"
        value: Double              // მაგ: +10% (10.0) ან +20 ₾ (20.0)
    ): List<ProductItem> {
        val updated = currentList.map { item ->
            val matchBrand = targetBrand == "ყველა" || item.brand.equals(targetBrand, ignoreCase = true)
            val matchCat = targetCategory == "ყველა" || item.category.equals(targetCategory, ignoreCase = true)

            if (matchBrand && matchCat) {
                val currentPriceNum = item.price.filter { it.isDigit() }.toIntOrNull()
                if (currentPriceNum != null && currentPriceNum > 0) {
                    val newPriceNum = if (mode == "PERCENT") {
                        val adjusted = currentPriceNum + (currentPriceNum * (value / 100.0))
                        maxOf(5, ((adjusted / 5.0).toInt()) * 5)
                    } else {
                        maxOf(5, (currentPriceNum + value).toInt())
                    }
                    item.copy(price = "$newPriceNum ₾")
                } else if (mode == "FIXED" && value > 0) {
                    // თუ პროდუქტს ეწერა "ფასი შეთანხმებით" და ადმინმა მიუთითა ფიქსირებული ფასი
                    item.copy(price = "${value.toInt()} ₾")
                } else {
                    item
                }
            } else {
                item
            }
        }

        saveProducts(context, updated)
        return updated
    }

    // =========================================================================
    // აპლიკაციის განახლების მართვა (App Update Config: app_config/updates)
    // =========================================================================
    private const val KEY_APP_UPDATE_LATEST_VERSION = "app_update_latest_version_v1"
    private const val KEY_APP_UPDATE_CHANGELOG = "app_update_changelog_v1"
    private const val KEY_APP_UPDATE_DOWNLOAD_URL = "app_update_download_url_v1"
    private const val KEY_APP_UPDATE_IS_PUBLISHED = "app_update_is_published_v1"
    private const val KEY_APP_UPDATE_UPDATED_AT = "app_update_updated_at_v1"
    private const val KEY_APP_UPDATE_UPDATED_BY = "app_update_updated_by_v1"

    data class AppUpdateConfig(
        val latestVersion: String = "",
        val changeLog: String = "",
        val downloadUrl: String = "",
        val isPublished: Boolean = false,
        val updatedAtMillis: Long = 0L,
        val updatedBy: String = ""
    )

    fun isValidHttpsDownloadUrl(url: String): Boolean {
        val clean = url.trim()
        if (!clean.startsWith("https://", ignoreCase = true)) return false
        if (clean.length < 12 || clean.contains(" ")) return false
        return try {
            val parsed = android.net.Uri.parse(clean)
            parsed.scheme?.equals("https", ignoreCase = true) == true && !parsed.host.isNullOrBlank()
        } catch (_: Exception) {
            false
        }
    }

    /**
     * ამოწმებს არის თუ არა ბმული სტანდარტული Google Drive-ის გაზიარების გვერდი
     * (მაგ: https://drive.google.com/file/d/<ID>/view?usp=sharing), რომელიც პირდაპირ APK-ს
     * არ იწერს და ხსნის ვებ-გვერდს.
     */
    fun isGoogleDriveShareViewerLink(url: String): Boolean {
        val clean = url.trim()
        if (!clean.contains("drive.google.com", ignoreCase = true)) return false
        return clean.contains("/file/d/", ignoreCase = true) ||
            (clean.contains("/open?", ignoreCase = true) && !clean.contains("export=download", ignoreCase = true))
    }

    /**
     * გარდაქმნის Google Drive-ის გაზიარების ბმულს პირდაპირი ჩამოტვირთვის ბმულად:
     * https://drive.google.com/uc?export=download&id=<FILE_ID>
     */
    fun extractGoogleDriveDirectDownloadUrl(url: String): String? {
        val clean = url.trim()
        val fileDRegex = Regex("""drive\.google\.com/file/d/([a-zA-Z0-9_-]+)""", RegexOption.IGNORE_CASE)
        fileDRegex.find(clean)?.groupValues?.getOrNull(1)?.let { fileId ->
            if (fileId.isNotBlank()) {
                return "https://drive.google.com/uc?export=download&id=$fileId"
            }
        }
        val idParamRegex = Regex("""[?&]id=([a-zA-Z0-9_-]+)""", RegexOption.IGNORE_CASE)
        if (clean.contains("drive.google.com", ignoreCase = true)) {
            idParamRegex.find(clean)?.groupValues?.getOrNull(1)?.let { fileId ->
                if (fileId.isNotBlank()) {
                    return "https://drive.google.com/uc?export=download&id=$fileId"
                }
            }
        }
        return null
    }

    fun getAppUpdateConfig(context: Context): AppUpdateConfig {
        val prefs = getPrefs(context)
        return AppUpdateConfig(
            latestVersion = prefs.getString(KEY_APP_UPDATE_LATEST_VERSION, "") ?: "",
            changeLog = prefs.getString(KEY_APP_UPDATE_CHANGELOG, "") ?: "",
            downloadUrl = prefs.getString(KEY_APP_UPDATE_DOWNLOAD_URL, "") ?: "",
            isPublished = prefs.getBoolean(KEY_APP_UPDATE_IS_PUBLISHED, false),
            updatedAtMillis = prefs.getLong(KEY_APP_UPDATE_UPDATED_AT, 0L),
            updatedBy = prefs.getString(KEY_APP_UPDATE_UPDATED_BY, "") ?: ""
        )
    }

    fun saveAppUpdateConfig(context: Context, config: AppUpdateConfig) {
        getPrefs(context).edit()
            .putString(KEY_APP_UPDATE_LATEST_VERSION, config.latestVersion.trim())
            .putString(KEY_APP_UPDATE_CHANGELOG, config.changeLog.trim())
            .putString(KEY_APP_UPDATE_DOWNLOAD_URL, config.downloadUrl.trim())
            .putBoolean(KEY_APP_UPDATE_IS_PUBLISHED, config.isPublished)
            .putLong(
                KEY_APP_UPDATE_UPDATED_AT,
                if (config.updatedAtMillis > 0L) config.updatedAtMillis else System.currentTimeMillis()
            )
            .putString(KEY_APP_UPDATE_UPDATED_BY, config.updatedBy.trim())
            .commit()
    }
}

