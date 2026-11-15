package com.example

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject

/**
 * ადმინისტრატორის მონაცემთა მართვა (PIN კოდი, პროდუქტების და პროგრამული სერვისების რედაქტირება,
 * მასიური ფასებისა და მარჟის ცვლილება, მონაცემთა სინქრონული შენახვა SharedPreferences-ში).
 */
object AdminStorage {
    private const val PREFS_NAME = "bekaservice_admin_prefs"
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

    const val DEFAULT_BOG_IBAN = "GE17BG0000000580620970"
    const val DEFAULT_BOG_RECIPIENT = "BSTORE"
    const val DEFAULT_TBC_IBAN = "GE65TB7777777000000001"
    const val DEFAULT_TBC_RECIPIENT = "BSTORE"

    data class BankRequisites(
        val bogIban: String = "",
        val bogRecipient: String = "",
        val tbcIban: String = "",
        val tbcRecipient: String = ""
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
            .apply()
    }

    fun isInitialCloudMigrationCompleted(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_INITIAL_CLOUD_MIGRATION_DONE, false)
    }

    fun setInitialCloudMigrationCompleted(context: Context, completed: Boolean) {
        getPrefs(context).edit()
            .putBoolean(KEY_INITIAL_CLOUD_MIGRATION_DONE, completed)
            .apply()
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
            .apply()
    }

    fun getLastLocalEditTimestamp(context: Context): Long {
        return getPrefs(context).getLong(KEY_LAST_LOCAL_EDIT_TS, 0L)
    }

    // =========================================================================
    // წაშლილი პროდუქტების ID-ების აღრიცხვა (რომ თავიდან გახსნისას არ აღდგეს)
    // =========================================================================
    @Volatile
    private var cachedRawProductsJson: String? = null
    @Volatile
    private var cachedParsedProducts: List<ProductItem>? = null
    @Volatile
    private var cachedRawBannersJson: String? = null
    @Volatile
    private var cachedParsedBanners: List<HomeBannerItem>? = null

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
        cachedParsedProducts = null
        getPrefs(context).edit()
            .putString(KEY_DELETED_PRODUCT_IDS, arr.toString())
            .putLong(KEY_LAST_LOCAL_EDIT_TS, System.currentTimeMillis())
            .apply()
    }

    fun unmarkProductDeleted(context: Context, productId: Int) {
        val current = getDeletedProductIds(context).toMutableSet()
        if (current.remove(productId)) {
            val arr = JSONArray()
            current.forEach { arr.put(it) }
            cachedParsedProducts = null
            getPrefs(context).edit()
                .putString(KEY_DELETED_PRODUCT_IDS, arr.toString())
                .apply()
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
            .apply()
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
    // ადმინისტრატორის ავტორიზაცია იმართება Firebase Auth + Custom Claims-ით
    // =========================================================================

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

        val existingParsed = cachedParsedBanners
        if (existingParsed != null && cachedRawBannersJson == jsonStr) {
            return existingParsed
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
            val sanitized = list.filterNot { ProductRepository.isProhibitedBanner(it) }
            val resolved = if (sanitized.isNotEmpty()) {
                sanitized
            } else {
                DEFAULT_HOME_BANNERS
            }
            cachedRawBannersJson = jsonStr
            cachedParsedBanners = resolved
            resolved
        } catch (e: Exception) {
            DEFAULT_HOME_BANNERS
        }
    }

    fun saveHomeBanners(context: Context, banners: List<HomeBannerItem>) {
        val sanitized = banners.filterNot { ProductRepository.isProhibitedBanner(it) }
        val jsonArray = JSONArray()
        for (b in sanitized) {
            val obj = JSONObject()
            obj.put("id", b.id)
            obj.put("imageUrl", b.imageUrl)
            obj.put("title", b.title)
            obj.put("subtitle", b.subtitle)
            obj.put("targetProductId", b.targetProductId)
            obj.put("tag", b.tag)
            jsonArray.put(obj)
        }
        val serialized = jsonArray.toString()
        cachedRawBannersJson = serialized
        cachedParsedBanners = null
        getPrefs(context).edit()
            .putString(KEY_HOME_BANNERS, serialized)
            .putBoolean(KEY_HAS_CUSTOM_BANNERS, true)
            .apply()
    }

    fun resetHomeBanners(context: Context): List<HomeBannerItem> {
        cachedRawBannersJson = null
        cachedParsedBanners = DEFAULT_HOME_BANNERS
        getPrefs(context).edit()
            .remove(KEY_HOME_BANNERS)
            .putBoolean(KEY_HAS_CUSTOM_BANNERS, false)
            .apply()
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
            if (item.id in deletedIds || ProductRepository.isProhibitedOrRetiredService(item)) continue
            val match = defaultById[item.id]
            val enriched = if (match != null) {
                item.copy(
                    id = match.id,
                    title = item.title.ifBlank { match.title },
                    price = item.price.ifBlank { match.price },
                    category = if (item.category.isBlank() || item.category == "სხვა") match.category else item.category,
                    type = "product",
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
                    brand = formatCanonicalBrandDisplayName(item.brand.ifBlank { match.brand }),
                    brandId = resolveCanonicalBrandId(item.brand.ifBlank { match.brand }, item.brandId.ifBlank { match.brandId }),
                    compatibleModels = item.compatibleModels.ifBlank { match.compatibleModels },
                    warranty = item.warranty.ifBlank { match.warranty },
                    brandColorHex = item.brandColorHex.ifBlank { match.brandColorHex },
                    brandBadgeText = item.brandBadgeText.ifBlank { match.brandBadgeText }
                )
            } else {
                val cleanBrand = formatCanonicalBrandDisplayName(item.brand)
                item.copy(
                    brand = cleanBrand,
                    brandId = resolveCanonicalBrandId(cleanBrand, item.brandId)
                )
            }
            if (!ProductRepository.isProhibitedOrRetiredService(enriched) && seenIds.add(enriched.id)) {
                merged.add(enriched)
            }
        }

        for (defItem in defaults) {
            if (defItem.id !in seenIds && defItem.id !in deletedIds && !ProductRepository.isProhibitedOrRetiredService(defItem)) {
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

        val effectiveDeletedIds = getDeletedProductIds(context) + ProductRepository.RETIRED_PRODUCT_IDS
        val filteredLocal = currentLocal.filter {
            it.id !in effectiveDeletedIds && !ProductRepository.isProhibitedOrRetiredService(it)
        }
        val cleanCloudProducts = cloudProducts.filterNot { ProductRepository.isProhibitedOrRetiredService(it) }

        if (cleanCloudProducts.isEmpty()) {
            return filteredLocal
        }

        val localById = filteredLocal.associateBy { it.id }.toMutableMap()
        val localByDocId = filteredLocal.filter { it.firestoreDocId.isNotBlank() }
            .associateBy { it.firestoreDocId }

        val newlyAddedFromCloud = mutableListOf<ProductItem>()

        val preserveUnsyncedLocal = hasUnsyncedLocalEdits(context)

        for (cloudItem in cleanCloudProducts) {
            if (cloudItem.id in effectiveDeletedIds || ProductRepository.isProhibitedOrRetiredService(cloudItem)) continue
            val deterministicDocId = cloudItem.firestoreDocId.ifBlank { "prod_${cloudItem.id}" }
            val existingLocal = localById[cloudItem.id] ?: localByDocId[deterministicDocId]

            if (existingLocal != null) {
                val targetId = existingLocal.id
                if (preserveUnsyncedLocal) {
                    localById[targetId] = existingLocal.copy(
                        firestoreDocId = deterministicDocId,
                        isFavorite = AuthAndOrderManager.isProductFavorite(context, targetId)
                    )
                } else {
                    localById[targetId] = existingLocal.copy(
                        id = targetId,
                        title = cloudItem.title.ifBlank { existingLocal.title },
                        price = cloudItem.price.ifBlank { existingLocal.price },
                        category = normalizeCategoryName(cloudItem.category.ifBlank { existingLocal.category }),
                        categoryId = resolveCanonicalCategoryId(
                            rawCategory = cloudItem.category.ifBlank { existingLocal.category },
                            rawProductType = cloudItem.productType.ifBlank { existingLocal.productType },
                            rawCategoryId = cloudItem.categoryId.ifBlank { existingLocal.categoryId }
                        ),
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
                        brand = formatCanonicalBrandDisplayName(cloudItem.brand.ifBlank { existingLocal.brand }),
                        brandId = resolveCanonicalBrandId(
                            cloudItem.brand.ifBlank { existingLocal.brand },
                            cloudItem.brandId.ifBlank { existingLocal.brandId }
                        ),
                        compatibleModels = cloudItem.compatibleModels.ifBlank { existingLocal.compatibleModels },
                        warranty = cloudItem.warranty.ifBlank { existingLocal.warranty },
                        firestoreDocId = deterministicDocId,
                        brandColorHex = cloudItem.brandColorHex.ifBlank { existingLocal.brandColorHex },
                        brandBadgeText = cloudItem.brandBadgeText.ifBlank { existingLocal.brandBadgeText },
                        images = if (cloudItem.images.isNotEmpty()) cloudItem.images else existingLocal.images,
                        primaryImageUrl = cloudItem.primaryImageUrl ?: existingLocal.primaryImageUrl,
                        specifications = if (cloudItem.specifications.isNotEmpty()) cloudItem.specifications else existingLocal.specifications,
                        productType = cloudItem.productType.ifBlank { existingLocal.productType },
                        condition = cloudItem.condition.ifBlank { existingLocal.condition },
                        exactModel = cloudItem.exactModel.ifBlank { existingLocal.exactModel },
                        originalPrice = cloudItem.originalPrice,
                        discountedPrice = cloudItem.discountedPrice,
                        discountPercent = cloudItem.discountPercent,
                        discountExpiresAt = cloudItem.discountExpiresAt,
                        isActive = cloudItem.isActive
                    )
                }
            } else {
                val resolvedCat = normalizeCategoryName(cloudItem.category)
                val resolvedBrand = formatCanonicalBrandDisplayName(cloudItem.brand)
                newlyAddedFromCloud.add(
                    cloudItem.copy(
                        category = resolvedCat,
                        categoryId = resolveCanonicalCategoryId(
                            rawCategory = resolvedCat,
                            rawProductType = cloudItem.productType,
                            rawCategoryId = cloudItem.categoryId
                        ),
                        brand = resolvedBrand,
                        brandId = resolveCanonicalBrandId(resolvedBrand, cloudItem.brandId),
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
            if (ProductRepository.isProhibitedOrRetiredService(newItem)) continue
            val docKey = newItem.firestoreDocId.ifBlank { "prod_${newItem.id}" }
            if (seenIds.add(newItem.id) && seenDocIds.add(docKey)) {
                finalResult.add(newItem)
            }
        }
        for (item in localById.values) {
            if (ProductRepository.isProhibitedOrRetiredService(item)) continue
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
                .map { if (it.isFavorite == (it.id in favoriteIds)) it else it.copy(isFavorite = it.id in favoriteIds) }
        }

        val existingParsed = cachedParsedProducts
        if (existingParsed != null && cachedRawProductsJson == jsonStr) {
            return existingParsed
                .filter { it.id !in deletedIds }
                .map { if (it.isFavorite == (it.id in favoriteIds)) it else it.copy(isFavorite = it.id in favoriteIds) }
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

                val imagesArr = obj.optJSONArray("images")
                val imagesList = mutableListOf<String>()
                if (imagesArr != null) {
                    for (k in 0 until imagesArr.length()) {
                        val img = imagesArr.optString(k, "").trim()
                        if (img.isNotBlank()) imagesList.add(img)
                    }
                }
                val specsObj = obj.optJSONObject("specifications")
                val specsMap = mutableMapOf<String, String>()
                if (specsObj != null) {
                    val keys = specsObj.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        specsMap[key] = specsObj.optString(key, "")
                    }
                }

                val rawCat = obj.optString("category", "სხვა")
                val rawProdType = obj.optString("productType", "")
                val rawCatId = obj.optString("categoryId", "")
                val normCat = resolveCanonicalCategoryName(rawCat, rawProdType, rawCatId)
                val normCatId = resolveCanonicalCategoryId(normCat, rawProdType, rawCatId)
                val candidate = ProductItem(
                    id = id,
                    title = obj.optString("title", ""),
                    price = obj.optString("price", "0 ₾"),
                    category = normCat,
                    categoryId = normCatId,
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
                    brand = formatCanonicalBrandDisplayName(obj.optString("brand", "Apple")),
                    brandId = resolveCanonicalBrandId(
                        obj.optString("brand", "Apple"),
                        obj.optString("brandId", "")
                    ),
                    compatibleModels = obj.optString("compatibleModels", ""),
                    warranty = obj.optString("warranty", "6 თვე"),
                    firestoreDocId = obj.optString("firestoreDocId", "prod_$id"),
                    brandColorHex = obj.optString("brandColorHex", "#FF7A00"),
                    brandBadgeText = obj.optString("brandBadgeText", ""),
                    images = imagesList,
                    primaryImageUrl = obj.optString("primaryImageUrl", "").ifBlank { null },
                    specifications = specsMap,
                    productType = rawProdType,
                    condition = obj.optString("condition", "ახალი"),
                    exactModel = obj.optString("exactModel", ""),
                    originalPrice = obj.optString("originalPrice", ""),
                    discountedPrice = obj.optString("discountedPrice", ""),
                    discountPercent = obj.optInt("discountPercent", 0),
                    discountExpiresAt = obj.optLong("discountExpiresAt", 0L).takeIf { it > 0L },
                    isActive = obj.optBoolean("isActive", true)
                )
                if (!ProductRepository.isProhibitedOrRetiredService(candidate)) {
                    list.add(candidate)
                }
            }
            if (list.isNotEmpty()) {
                val merged = mergeWithCatalogDefaults(list, deletedIds)
                cachedRawProductsJson = jsonStr
                cachedParsedProducts = merged
                merged.map { if (it.isFavorite == (it.id in favoriteIds)) it else it.copy(isFavorite = it.id in favoriteIds) }
            } else {
                ProductRepository.getAllProducts()
                    .filter { it.id !in deletedIds }
                    .map { if (it.isFavorite == (it.id in favoriteIds)) it else it.copy(isFavorite = it.id in favoriteIds) }
            }
        } catch (e: Exception) {
            ProductRepository.getAllProducts()
                .filter { it.id !in deletedIds }
                .map { if (it.isFavorite == (it.id in favoriteIds)) it else it.copy(isFavorite = it.id in favoriteIds) }
        }
    }

    private fun serializeProductsToJson(products: List<ProductItem>): String {
        val jsonArray = JSONArray()
        val seenIds = mutableSetOf<Int>()
        for (item in products) {
            if (ProductRepository.isProhibitedOrRetiredService(item)) continue
            if (!seenIds.add(item.id)) continue
            val obj = JSONObject()
            val resolvedCatName = resolveCanonicalCategoryName(item.category, item.productType, item.categoryId)
            val resolvedCatId = resolveCanonicalCategoryId(resolvedCatName, item.productType, item.categoryId)
            obj.put("id", item.id)
            obj.put("title", item.title)
            obj.put("price", item.price)
            obj.put("category", resolvedCatName)
            obj.put("categoryId", resolvedCatId)
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
            val canonicalBrand = formatCanonicalBrandDisplayName(item.brand)
            obj.put("inStock", item.inStock)
            obj.put("brand", canonicalBrand)
            obj.put("brandId", resolveCanonicalBrandId(canonicalBrand, item.brandId))
            obj.put("compatibleModels", item.compatibleModels)
            obj.put("warranty", item.warranty)
            obj.put("firestoreDocId", item.firestoreDocId.ifBlank { "prod_${item.id}" })
            obj.put("brandColorHex", item.brandColorHex)
            obj.put("brandBadgeText", item.brandBadgeText)
            if (item.images.isNotEmpty()) {
                obj.put("images", JSONArray(item.images))
            }
            if (!item.primaryImageUrl.isNullOrBlank()) {
                obj.put("primaryImageUrl", item.primaryImageUrl)
            }
            if (item.specifications.isNotEmpty()) {
                obj.put("specifications", JSONObject(item.specifications))
            }
            obj.put("productType", item.productType)
            obj.put("condition", item.condition)
            obj.put("exactModel", item.exactModel)
            obj.put("originalPrice", item.originalPrice)
            obj.put("discountedPrice", item.discountedPrice)
            obj.put("discountPercent", item.discountPercent)
            obj.put("isActive", item.isActive)
            if (item.discountExpiresAt != null && item.discountExpiresAt > 0L) {
                obj.put("discountExpiresAt", item.discountExpiresAt)
            }
            jsonArray.put(obj)
        }
        return jsonArray.toString()
    }

    /**
     * ინახავს Firestore-დან სინქრონიზებულ კატალოგს ლოკალურ ქეშში (ოფლაინ რეჟიმისთვის),
     * ზედმეტი განმეორებითი ჩაწერის გარეშე.
     */
    fun saveProductsFromCloud(context: Context, products: List<ProductItem>) {
        val sanitized = products.filterNot { ProductRepository.isProhibitedOrRetiredService(it) }
        val prefs = getPrefs(context)
        val serialized = serializeProductsToJson(sanitized)
        val existingJson = prefs.getString(KEY_CUSTOM_PRODUCTS, null)
        if (existingJson == serialized && prefs.getBoolean(KEY_HAS_CUSTOM_PRODUCTS, false)) {
            return
        }
        cachedRawProductsJson = serialized
        cachedParsedProducts = sanitized
        val editor = prefs.edit()
            .putBoolean(KEY_HAS_CUSTOM_PRODUCTS, true)
            .putString(KEY_CUSTOM_PRODUCTS, serialized)
        if (!prefs.getBoolean(KEY_HAS_UNSYNCED_LOCAL_EDITS, false)) {
            editor.putBoolean(KEY_HAS_UNSYNCED_LOCAL_EDITS, false)
        }
        editor.apply()
    }

    fun saveProducts(context: Context, products: List<ProductItem>, markUnsyncedLocalEdit: Boolean = false) {
        val sanitized = products.filterNot { ProductRepository.isProhibitedOrRetiredService(it) }
        val serialized = serializeProductsToJson(sanitized)
        cachedRawProductsJson = serialized
        cachedParsedProducts = sanitized
        getPrefs(context).edit()
            .putBoolean(KEY_HAS_CUSTOM_PRODUCTS, true)
            .putBoolean(KEY_HAS_UNSYNCED_LOCAL_EDITS, markUnsyncedLocalEdit)
            .putString(KEY_CUSTOM_PRODUCTS, serialized)
            .putLong(KEY_LAST_LOCAL_EDIT_TS, System.currentTimeMillis())
            .apply()
    }

    fun resetToDefaults(context: Context): List<ProductItem> {
        cachedRawProductsJson = null
        cachedParsedProducts = null
        getPrefs(context).edit()
            .remove(KEY_HAS_CUSTOM_PRODUCTS)
            .remove(KEY_HAS_UNSYNCED_LOCAL_EDITS)
            .remove(KEY_CUSTOM_PRODUCTS)
            .remove(KEY_DELETED_PRODUCT_IDS)
            .remove(KEY_KNOWN_CLOUD_DOC_IDS)
            .putLong(KEY_LAST_LOCAL_EDIT_TS, System.currentTimeMillis())
            .apply()
        val favoriteIds = AuthAndOrderManager.getFavoriteProductIds(context)
        return ProductRepository.getAllProducts().map { it.copy(isFavorite = it.id in favoriteIds) }
    }

    /**
     * უსაფრთხო ახალი პროდუქტის ID-ის გენერაცია:
     * გამორიცხავს კოლიზიას როგორც საწყის 1..85 კატალოგთან, ისე ProductRepository.RETIRED_PRODUCT_IDS (86..124) დიაპაზონთან.
     * ყველა ახალი პროდუქტის ID გარანტირებულად იწყება 201-დან და იზრდება მონოტონურად.
     */
    fun allocateNextProductId(existingProducts: List<ProductItem>): Int {
        val maxExistingId = existingProducts
            .filterNot { it.id in ProductRepository.RETIRED_PRODUCT_IDS }
            .maxOfOrNull { it.id } ?: 200
        var candidate = maxOf(maxExistingId + 1, 201)
        val usedIds = existingProducts.map { it.id }.toSet() + ProductRepository.RETIRED_PRODUCT_IDS
        while (candidate in usedIds) {
            candidate++
        }
        return candidate
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

    // =========================================================================
    // დინამიური კატეგორიების მართვა (Canonical Policy + CRUD + Auto-Registration + ProductType Engine)
    // =========================================================================
    private const val KEY_CUSTOM_CATEGORIES = "custom_categories_json"
    private const val KEY_CATEGORIES_POLICY_VERSION = "categories_policy_version"
    private const val CURRENT_CATEGORIES_POLICY_VERSION = 3
    private const val KEY_ADMIN_SESSION_ACTIVE = "admin_session_active"
    private const val KEY_ADMIN_SESSION_TS = "admin_session_ts"
    private const val KEY_ADMIN_SESSION_EMAIL = "admin_session_email"

    var categoriesVersion by mutableIntStateOf(0)
        private set

    enum class AssignmentConfidence {
        EXACT_TYPE,
        HIGH_CONFIDENCE_TEXT,
        AMBIGUOUS_SUGGESTION,
        MANUAL_OVERRIDE,
        UNRESOLVED
    }

    data class CategoryAssignmentAnalysis(
        val detectedProductType: String,
        val canonicalCategoryId: String,
        val canonicalCategoryName: String,
        val confidence: AssignmentConfidence,
        val requiresAdminConfirmation: Boolean,
        val candidateCategories: List<String> = emptyList(),
        val reasonGeo: String = ""
    ) {
        val categoryId: String get() = canonicalCategoryId
        val categoryName: String get() = canonicalCategoryName
        val productType: String get() = detectedProductType
    }

    fun normalizeCategoryName(raw: String): String {
        val clean = raw.trim().replace(Regex("\\s+"), " ")
        if (clean.isBlank()) return ""
        return when (clean.lowercase()) {
            "როუტერები & ქსელი", "როუტერები და ქსელები", "ქსელური მოწყობილობები", "როუტერები", "wi-fi როუტერები" -> "როუტერები და ქსელი"
            "მობილურები", "სმარტფონები", "ტელეფონები", "მობილური ტელეფონი" -> "მობილური ტელეფონები"
            "ყურსასმენები", "ყურსასმენები და აუდიო", "ყურსასმენები & აუდიო", "აუდიო", "ყურსასმენი", "airpods", "earbuds", "headphones" -> "ყურსასმენები"
            "დამტენები", "დამტენები & კაბელები", "კაბელები", "დამტენი", "კაბელი", "usb კაბელი" -> "დამტენები და კაბელები"
            "დამტენი პორტები", "დამტენის პორტი" -> "დამტენის პორტები"
            "კლავიატურა", "კლავიატურები", "keyboard", "keyboards" -> "კლავიატურები"
            "მაუსი", "მაუსები", "თაგვი", "mouse", "mice" -> "მაუსები"
            "ქეისები", "ქეისი", "ბრონები", "დამცავი მინები", "ტელეფონის აქსესუარები" -> "აქსესუარები"
            "ეკრანი", "დისპლეი", "დისპლეები" -> "ეკრანები"
            "ელემენტი", "ბატარეა", "ბატარეები" -> "ელემენტები"
            "კამერა" -> "კამერები"
            "დინამიკი" -> "დინამიკები"
            "კორპუსი", "უკანა შუშები" -> "კორპუსები"
            else -> clean
        }
    }

    fun resolveCanonicalCategoryId(
        rawCategory: String,
        rawProductType: String = "",
        rawCategoryId: String = ""
    ): String {
        val cleanId = rawCategoryId.trim().lowercase()
        when (cleanId) {
            "cat_screens" -> return "cat_screens"
            "cat_batteries" -> return "cat_batteries"
            "cat_mobiles" -> return "cat_mobiles"
            "cat_headphones", "cat_audio" -> return "cat_headphones"
            "cat_chargers" -> return "cat_chargers"
            "cat_routers" -> return "cat_routers"
            "cat_cameras" -> return "cat_cameras"
            "cat_speakers" -> return "cat_speakers"
            "cat_charge_ports" -> return "cat_charge_ports"
            "cat_housings" -> return "cat_housings"
            "cat_accessories" -> return "cat_accessories"
            "cat_keyboards" -> return "cat_keyboards"
            "cat_mice" -> return "cat_mice"
            "cat_electronics" -> return "cat_electronics"
        }

        val normName = normalizeCategoryName(rawCategory)
        when (normName) {
            "ეკრანები" -> return "cat_screens"
            "ელემენტები" -> return "cat_batteries"
            "მობილური ტელეფონები" -> return "cat_mobiles"
            "ყურსასმენები" -> return "cat_headphones"
            "დამტენები და კაბელები" -> return "cat_chargers"
            "როუტერები და ქსელი" -> return "cat_routers"
            "კამერები" -> return "cat_cameras"
            "დინამიკები" -> return "cat_speakers"
            "დამტენის პორტები" -> return "cat_charge_ports"
            "კორპუსები" -> return "cat_housings"
            "აქსესუარები" -> return "cat_accessories"
            "კლავიატურები" -> return "cat_keyboards"
            "მაუსები" -> return "cat_mice"
            "ელექტრონიკა" -> return "cat_electronics"
        }

        val cleanType = rawProductType.trim().lowercase()
        when (cleanType) {
            "replacement_display", "display", "screen" -> return "cat_screens"
            "battery", "batteries" -> return "cat_batteries"
            "mobile_phone", "smartphone", "phone", "mobile" -> return "cat_mobiles"
            "headphones", "earbuds", "headset", "audio", "earphones" -> return "cat_headphones"
            "charger_cable", "charger", "cable" -> return "cat_chargers"
            "networking", "router", "modem", "wifi" -> return "cat_routers"
            "camera", "cameras" -> return "cat_cameras"
            "speaker", "speakers" -> return "cat_speakers"
            "charging_port", "charge_port" -> return "cat_charge_ports"
            "housing", "back_glass" -> return "cat_housings"
            "phone_accessory", "accessory", "phone_case", "case", "screen_protector" -> return "cat_accessories"
            "keyboard", "keyboards" -> return "cat_keyboards"
            "mouse", "mice" -> return "cat_mice"
            "tablet", "smartwatch", "other_electronics", "electronics" -> return "cat_electronics"
        }

        if (cleanId.isNotBlank()) return cleanId
        if (normName.isNotBlank()) return "cat_custom_${kotlin.math.abs(normName.lowercase().hashCode())}"
        return "cat_electronics"
    }

    fun resolveCanonicalCategoryName(
        rawCategory: String,
        rawProductType: String = "",
        rawCategoryId: String = ""
    ): String {
        val norm = normalizeCategoryName(rawCategory)
        if (norm.isNotBlank() && norm != "სხვა") {
            return norm
        }
        return when (resolveCanonicalCategoryId(rawCategory, rawProductType, rawCategoryId)) {
            "cat_screens" -> "ეკრანები"
            "cat_batteries" -> "ელემენტები"
            "cat_mobiles" -> "მობილური ტელეფონები"
            "cat_headphones" -> "ყურსასმენები"
            "cat_chargers" -> "დამტენები და კაბელები"
            "cat_routers" -> "როუტერები და ქსელი"
            "cat_cameras" -> "კამერები"
            "cat_speakers" -> "დინამიკები"
            "cat_charge_ports" -> "დამტენის პორტები"
            "cat_housings" -> "კორპუსები"
            "cat_accessories" -> "აქსესუარები"
            "cat_keyboards" -> "კლავიატურები"
            "cat_mice" -> "მაუსები"
            "cat_electronics" -> "ელექტრონიკა"
            else -> norm.ifBlank { "ელექტრონიკა" }
        }
    }

    /**
     * ამოიცნობს პროდუქტის ტიპს და ავტომატურად აკავშირებს კანონიკურ კატეგორიასთან.
     * თუ სათაური/აღწერა ორაზროვანია (მაგ: რამდენიმე სხვადასხვა კატეგორიის ნიშანი აქვს ან ბუნდოვანია),
     * აბრუნებს requiresAdminConfirmation = true და სთავაზობს კატეგორიას ადმინისტრატორის დადასტურებით.
     */
    fun detectProductCategoryAssignment(
        title: String,
        description: String = "",
        explicitProductType: String = "",
        manualCategoryOverride: String = ""
    ): CategoryAssignmentAnalysis {
        // 1. თუ ადმინისტრატორმა ხელით აირჩია კატეგორია (Manual Override), პატივს ვცემთ მის არჩევანს
        val cleanOverride = normalizeCategoryName(manualCategoryOverride)
        if (cleanOverride.isNotBlank()) {
            val catId = resolveCanonicalCategoryId(rawCategory = cleanOverride, rawProductType = explicitProductType)
            val meta = inferCategoryMetadata(cleanOverride)
            return CategoryAssignmentAnalysis(
                detectedProductType = explicitProductType.ifBlank { meta.productType },
                canonicalCategoryId = catId,
                canonicalCategoryName = cleanOverride,
                confidence = AssignmentConfidence.MANUAL_OVERRIDE,
                requiresAdminConfirmation = false,
                candidateCategories = listOf(cleanOverride),
                reasonGeo = "არჩეულია ადმინისტრატორის მიერ"
            )
        }

        // 2. თუ მითითებულია ცალსახა სტრუქტურირებული productType
        val cleanType = explicitProductType.trim().lowercase()
        val exactTypeMapping: Triple<String, String, String>? = when (cleanType) {
            "headphones", "earbuds", "headset", "earphones", "audio" -> Triple("headphones", "cat_headphones", "ყურსასმენები")
            "keyboard", "keyboards" -> Triple("keyboard", "cat_keyboards", "კლავიატურები")
            "mouse", "mice" -> Triple("mouse", "cat_mice", "მაუსები")
            "charger", "cable", "charger_cable" -> Triple("charger_cable", "cat_chargers", "დამტენები და კაბელები")
            "phone_case", "screen_protector", "phone_accessory", "accessory", "case" -> Triple("phone_accessory", "cat_accessories", "აქსესუარები")
            "smartphone", "mobile_phone", "phone", "mobile" -> Triple("mobile_phone", "cat_mobiles", "მობილური ტელეფონები")
            "replacement_display", "display", "screen" -> Triple("replacement_display", "cat_screens", "ეკრანები")
            "battery", "batteries" -> Triple("battery", "cat_batteries", "ელემენტები")
            "camera", "cameras" -> Triple("camera", "cat_cameras", "კამერები")
            "speaker", "speakers" -> Triple("speaker", "cat_speakers", "დინამიკები")
            "charging_port", "charge_port" -> Triple("charging_port", "cat_charge_ports", "დამტენის პორტები")
            "housing", "back_glass" -> Triple("housing", "cat_housings", "კორპუსები")
            "networking", "router", "modem", "wifi" -> Triple("networking", "cat_routers", "როუტერები და ქსელი")
            "tablet", "smartwatch" -> Triple(cleanType, "cat_electronics", "ელექტრონიკა")
            else -> null
        }

        // 3. ტექსტური სიგნალების ანალიზი სათაურიდან და აღწერიდან
        val combinedText = "${title.trim()} ${description.trim()}".lowercase()
        val matchedSignals = mutableListOf<Triple<String, String, String>>()

        if (combinedText.contains("ყურსასმენ") || combinedText.contains("airpods") ||
            combinedText.contains("earbuds") || combinedText.contains("headphone") ||
            combinedText.contains("headset") || combinedText.contains("galaxy buds") ||
            combinedText.contains("tws")
        ) {
            matchedSignals.add(Triple("headphones", "cat_headphones", "ყურსასმენები"))
        }
        if (combinedText.contains("კლავიატურ") || combinedText.contains("keyboard") || combinedText.contains("keycap")) {
            matchedSignals.add(Triple("keyboard", "cat_keyboards", "კლავიატურები"))
        }
        if (combinedText.contains("მაუს") || combinedText.contains("mouse") || combinedText.contains("თაგვ")) {
            matchedSignals.add(Triple("mouse", "cat_mice", "მაუსები"))
        }
        if ((combinedText.contains("დამტენ") && !combinedText.contains("პორტ")) ||
            combinedText.contains("კაბელ") || combinedText.contains("ადაპტერ") ||
            combinedText.contains("charger") || combinedText.contains("usb-c cable") ||
            combinedText.contains("lightning cable") || combinedText.contains("gan ")
        ) {
            matchedSignals.add(Triple("charger_cable", "cat_chargers", "დამტენები და კაბელები"))
        }
        if (combinedText.contains("ეკრან") || combinedText.contains("დისპლე") ||
            combinedText.contains("oled") || combinedText.contains("incell") ||
            combinedText.contains("display") || combinedText.contains("screen")
        ) {
            matchedSignals.add(Triple("replacement_display", "cat_screens", "ეკრანები"))
        }
        if (combinedText.contains("ელემენტ") || combinedText.contains("ბატარე") || combinedText.contains("battery")) {
            matchedSignals.add(Triple("battery", "cat_batteries", "ელემენტები"))
        }
        if (combinedText.contains("როუტერ") || combinedText.contains("router") ||
            combinedText.contains("modem") || combinedText.contains("4g lte") ||
            combinedText.contains("5g cpe") || combinedText.contains("b535")
        ) {
            matchedSignals.add(Triple("networking", "cat_routers", "როუტერები და ქსელი"))
        }
        if (combinedText.contains("კამერ") || combinedText.contains("camera")) {
            matchedSignals.add(Triple("camera", "cat_cameras", "კამერები"))
        }
        if (combinedText.contains("დინამიკ") || combinedText.contains("speaker") || combinedText.contains("დინამიკი")) {
            matchedSignals.add(Triple("speaker", "cat_speakers", "დინამიკები"))
        }
        if (combinedText.contains("დამტენის პორტ") || combinedText.contains("დამტენი პორტ") ||
            combinedText.contains("charging port") || combinedText.contains("შლეიფ")
        ) {
            matchedSignals.add(Triple("charging_port", "cat_charge_ports", "დამტენის პორტები"))
        }
        if (combinedText.contains("კორპუს") || combinedText.contains("უკანა შუშ") ||
            combinedText.contains("უკანა მინ") || combinedText.contains("housing")
        ) {
            matchedSignals.add(Triple("housing", "cat_housings", "კორპუსები"))
        }
        if (combinedText.contains("ქეის") || combinedText.contains("ბრონ") ||
            combinedText.contains("დამცავი მინ") || combinedText.contains("magsafe case") ||
            combinedText.contains("screen protector") || combinedText.contains("სამაგრ")
        ) {
            matchedSignals.add(Triple("phone_accessory", "cat_accessories", "აქსესუარები"))
        }

        val hasPhoneStoragePattern = Regex("""\b(\d{1,2}\s*/\s*\d{2,4}\s*gb|\d{2,4}\s*gb|1\s*tb)\b""", RegexOption.IGNORE_CASE).containsMatchIn(combinedText)
        val hasPhoneKeyword = combinedText.contains("სმარტფონ") || combinedText.contains("ტელეფონი") ||
            (hasPhoneStoragePattern && (combinedText.contains("iphone") || combinedText.contains("galaxy") || combinedText.contains("xiaomi") || combinedText.contains("pixel")))
        if (hasPhoneKeyword && matchedSignals.isEmpty()) {
            matchedSignals.add(Triple("mobile_phone", "cat_mobiles", "მობილური ტელეფონები"))
        }

        val distinctCategories = matchedSignals.distinctBy { it.second }

        // თუ სათაურში 2 ან მეტი ურთიერთგანსხვავებული კატეგორიაა ნახსენები (მაგ: "ეკრანი და ელემენტი" ან "ყურსასმენი და დამტენი"),
        // ეს არის ორაზროვანი (Ambiguous) შემთხვევა და მოითხოვს ადმინის დადასტურებას!
        if (distinctCategories.size >= 2 && exactTypeMapping == null) {
            val primary = distinctCategories.first()
            return CategoryAssignmentAnalysis(
                detectedProductType = primary.first,
                canonicalCategoryId = primary.second,
                canonicalCategoryName = primary.third,
                confidence = AssignmentConfidence.AMBIGUOUS_SUGGESTION,
                requiresAdminConfirmation = true,
                candidateCategories = distinctCategories.map { it.third },
                reasonGeo = "სათაურში აღმოჩენილია რამდენიმე კატეგორიის ნიშანი (${distinctCategories.joinToString(", ") { it.third }}). გთხოვთ დაადასტუროთ კატეგორია."
            )
        }

        if (exactTypeMapping != null) {
            return CategoryAssignmentAnalysis(
                detectedProductType = exactTypeMapping.first,
                canonicalCategoryId = exactTypeMapping.second,
                canonicalCategoryName = exactTypeMapping.third,
                confidence = AssignmentConfidence.EXACT_TYPE,
                requiresAdminConfirmation = false,
                candidateCategories = listOf(exactTypeMapping.third),
                reasonGeo = "ავტომატურად განისაზღვრა პროდუქტის ტიპით (${exactTypeMapping.third})"
            )
        }

        if (distinctCategories.size == 1) {
            val match = distinctCategories.first()
            return CategoryAssignmentAnalysis(
                detectedProductType = match.first,
                canonicalCategoryId = match.second,
                canonicalCategoryName = match.third,
                confidence = AssignmentConfidence.HIGH_CONFIDENCE_TEXT,
                requiresAdminConfirmation = false,
                candidateCategories = listOf(match.third),
                reasonGeo = "ავტომატურად ამოpoznობილია დასახელებიდან: ${match.third}"
            )
        }

        // თუ ვერც სტრუქტურირებული ტიპით და ვერც სათაურით ვერ განისაზღვრა ცალსახად:
        if (title.trim().isNotBlank()) {
            return CategoryAssignmentAnalysis(
                detectedProductType = "other_electronics",
                canonicalCategoryId = "cat_electronics",
                canonicalCategoryName = "ელექტრონიკა",
                confidence = AssignmentConfidence.AMBIGUOUS_SUGGESTION,
                requiresAdminConfirmation = true,
                candidateCategories = listOf("ელექტრონიკა", "აქსესუარები", "ეკრანები", "ყურსასმენები"),
                reasonGeo = "პროდუქტის ტიპი სათაურიდან ცალსახად არ იკითხება. შემოთავაზებულია „ელექტრონიკა“ — გთხოვთ დაადასტუროთ ან აირჩიოთ კატეგორია."
            )
        }

        return CategoryAssignmentAnalysis(
            detectedProductType = "",
            canonicalCategoryId = "",
            canonicalCategoryName = "",
            confidence = AssignmentConfidence.UNRESOLVED,
            requiresAdminConfirmation = true,
            candidateCategories = emptyList(),
            reasonGeo = "მიუთითეთ პროდუქტის დასახელება ან აირჩიეთ კატეგორია"
        )
    }

    /**
     * ამოწმებს, რომ პროდუქტს შენახვამდე აქვს ვალიდური კანონიკური კატეგორია და არ არის ობოლი/არასწორი ბმული.
     */
    fun validateProductCategoryAssignment(
        context: Context,
        categoryName: String,
        requiresUnconfirmedAmbiguity: Boolean
    ): Pair<Boolean, String> {
        if (requiresUnconfirmedAmbiguity) {
            return false to "გთხოვთ დაადასტუროთ ან აირჩიოთ პროდუქტის კატეგორია"
        }
        val res = validateProductCategoryAssignment(context, categoryName, "", "")
        return if (res.isSuccess) {
            true to (res.getOrNull()?.name ?: "")
        } else {
            false to (res.exceptionOrNull()?.message ?: "არასწორი კატეგორია")
        }
    }

    fun validateProductCategoryAssignment(
        context: Context,
        categoryName: String,
        categoryId: String = "",
        productType: String = ""
    ): Result<CategoryItem> {
        val normName = normalizeCategoryName(categoryName)
        if (normName.isBlank() || normName.equals("სხვა", ignoreCase = true) || normName.equals("უცნობი", ignoreCase = true)) {
            return Result.failure(IllegalArgumentException("INVALID_CATEGORY: გთხოვთ მიუთითოთ ან დაადასტუროთ ვალიდური კატეგორია."))
        }
        val ensured = ensureCategoryExistsForProduct(context, normName, productType)
            ?: return Result.failure(IllegalArgumentException("PROHIBITED_CATEGORY: არჩეული კატეგორია დაუშვებელია."))
        if (categoryId.isNotBlank() && categoryId != ensured.id && resolveCanonicalCategoryId(normName, productType, categoryId) != ensured.id) {
            return Result.failure(IllegalArgumentException("CATEGORY_ID_MISMATCH: კატეგორიის ID ვერ დადასტურდა."))
        }
        return Result.success(ensured)
    }

    data class CategoryMeta(
        val id: String,
        val slug: String,
        val productType: String,
        val icon: String,
        val localizedName: String,
        val categoryType: String,
        val description: String
    )

    fun inferCategoryMetadata(categoryName: String): CategoryMeta {
        val normalized = normalizeCategoryName(categoryName)
        return when (normalized) {
            "ეკრანები" -> CategoryMeta("cat_screens", "screens", "replacement_display", "📱", "Screens & OLED", "spare_part", "Super Retina OLED, Original & Premium Incell დისპლეები")
            "ელემენტები" -> CategoryMeta("cat_batteries", "batteries", "battery", "🔋", "Batteries", "spare_part", "100% Battery Health ორიგინალი და გაზრდილი ტევადობის ელემენტები")
            "მობილური ტელეფონები" -> CategoryMeta("cat_mobiles", "mobile_phones", "mobile_phone", "📲", "Mobile Phones", "mobile", "ახალი და შემოწმებული მეორადი სმარტფონები გარანტიით")
            "ყურსასმენები" -> CategoryMeta("cat_headphones", "headphones", "headphones", "🎧", "Headphones & Audio", "audio", "უკაბელო ყურსასმენები, TWS AirPods, Galaxy Buds და აუდიო მოწყობილობები")
            "დამტენები და კაბელები" -> CategoryMeta("cat_chargers", "chargers_cables", "charger_cable", "⚡", "Chargers & Cables", "accessory", "სწრაფი GaN ადაპტერები, MagSafe და ორიგინალი Type-C / Lightning კაბელები")
            "როუტერები და ქსელი" -> CategoryMeta("cat_routers", "routers_network", "networking", "📡", "Routers & Network", "networking", "4G/5G მობილური როუტერები, გარე ანტენები და ქსელური მოწყობილობები")
            "კამერები" -> CategoryMeta("cat_cameras", "cameras", "camera", "📸", "Cameras", "spare_part", "მთავარი და სელფის კამერის ორიგინალი მოდულები")
            "დინამიკები" -> CategoryMeta("cat_speakers", "speakers", "speaker", "🔊", "Speakers", "spare_part", "სასაუბრო და მუსიკალური სტერეო დინამიკები")
            "დამტენის პორტები" -> CategoryMeta("cat_charge_ports", "charging_ports", "charging_port", "🔌", "Charging Ports", "spare_part", "Lightning / Type-C პორტები და ქვედა მიკროფონის პლატები")
            "კორპუსები" -> CategoryMeta("cat_housings", "housings", "housing", "🛠️", "Housings & Glass", "spare_part", "უკანა შუშები, ტიტანის ჩარჩოები და ღილაკების კომპლექტები")
            "აქსესუარები" -> CategoryMeta("cat_accessories", "accessories", "phone_accessory", "🎒", "Accessories", "accessory", "დამცავი მინები, ქეისები და ყოველდღიური აქსესუარები")
            "კლავიატურები" -> CategoryMeta("cat_keyboards", "keyboards", "keyboard", "⌨️", "Keyboards", "accessory", "მექანიკური, უკაბელო და სამუშაო კლავიატურები")
            "მაუსები" -> CategoryMeta("cat_mice", "mice", "mouse", "🖱️", "Mice", "accessory", "უკაბელო, გეიმინგ და ერგონომიული მაუსები")
            "ელექტრონიკა" -> CategoryMeta("cat_electronics", "electronics", "other_electronics", "🎛️", "Electronics", "electronics", "სმარტ გაჯეტები და ტექნიკური ელექტრონიკა")
            else -> {
                val slug = normalized.lowercase().replace(Regex("[^a-zA-Z0-9ა-ჰ]+"), "_").trim('_')
                CategoryMeta(
                    id = "cat_custom_${kotlin.math.abs(normalized.lowercase().hashCode())}",
                    slug = slug.ifBlank { "custom" },
                    productType = "other_electronics",
                    icon = "📦",
                    localizedName = normalized,
                    categoryType = "general",
                    description = "$normalized — BSTORE-ის კატეგორია"
                )
            }
        }
    }

    fun getDefaultCategories(): List<CategoryItem> = listOf(
        CategoryItem(
            id = "cat_screens",
            name = "ეკრანები",
            slug = "screens",
            productType = "replacement_display",
            localizedName = "Screens & OLED",
            description = "Super Retina OLED, Original & Premium Incell დისპლეები",
            icon = "📱",
            displayOrder = 1,
            isActive = true,
            isVisible = true,
            isPopular = true,
            popularOrder = 1,
            categoryType = "spare_part"
        ),
        CategoryItem(
            id = "cat_batteries",
            name = "ელემენტები",
            slug = "batteries",
            productType = "battery",
            localizedName = "Batteries",
            description = "100% Battery Health ორიგინალი და გაზრდილი ტევადობის ელემენტები",
            icon = "🔋",
            displayOrder = 2,
            isActive = true,
            isVisible = true,
            isPopular = true,
            popularOrder = 2,
            categoryType = "spare_part"
        ),
        CategoryItem(
            id = "cat_mobiles",
            name = "მობილური ტელეფონები",
            slug = "mobile_phones",
            productType = "mobile_phone",
            localizedName = "Mobile Phones",
            description = "ახალი და შემოწმებული მეორადი სმარტფონები გარანტიით",
            icon = "📲",
            displayOrder = 3,
            isActive = true,
            isVisible = true,
            isPopular = true,
            popularOrder = 3,
            categoryType = "mobile"
        ),
        CategoryItem(
            id = "cat_headphones",
            name = "ყურსასმენები",
            slug = "headphones",
            productType = "headphones",
            localizedName = "Headphones & Audio",
            description = "უკაბელო ყურსასმენები, TWS AirPods, Galaxy Buds და აუდიო მოწყობილობები",
            icon = "🎧",
            displayOrder = 4,
            isActive = true,
            isVisible = true,
            isPopular = true,
            popularOrder = 4,
            categoryType = "audio"
        ),
        CategoryItem(
            id = "cat_chargers",
            name = "დამტენები და კაბელები",
            slug = "chargers_cables",
            productType = "charger_cable",
            localizedName = "Chargers & Cables",
            description = "სწრაფი GaN ადაპტერები, MagSafe და ორიგინალი Type-C / Lightning კაბელები",
            icon = "⚡",
            displayOrder = 5,
            isActive = true,
            isVisible = true,
            isPopular = true,
            popularOrder = 5,
            categoryType = "accessory"
        ),
        CategoryItem(
            id = "cat_routers",
            name = "როუტერები და ქსელი",
            slug = "routers_network",
            productType = "networking",
            localizedName = "Routers & Network",
            description = "4G/5G მობილური როუტერები, გარე ანტენები და ქსელური მოწყობილობები",
            icon = "📡",
            displayOrder = 6,
            isActive = true,
            isVisible = true,
            isPopular = true,
            popularOrder = 6,
            categoryType = "networking"
        ),
        CategoryItem(
            id = "cat_cameras",
            name = "კამერები",
            slug = "cameras",
            productType = "camera",
            localizedName = "Cameras",
            description = "მთავარი და სელფის კამერის ორიგინალი მოდულები",
            icon = "📸",
            displayOrder = 7,
            isActive = true,
            isVisible = true,
            isPopular = false,
            popularOrder = 7,
            categoryType = "spare_part"
        ),
        CategoryItem(
            id = "cat_speakers",
            name = "დინამიკები",
            slug = "speakers",
            productType = "speaker",
            localizedName = "Speakers",
            description = "სასაუბრო და მუსიკალური სტერეო დინამიკები",
            icon = "🔊",
            displayOrder = 8,
            isActive = true,
            isVisible = true,
            isPopular = false,
            popularOrder = 8,
            categoryType = "spare_part"
        ),
        CategoryItem(
            id = "cat_charge_ports",
            name = "დამტენის პორტები",
            slug = "charging_ports",
            productType = "charging_port",
            localizedName = "Charging Ports",
            description = "Lightning / Type-C პორტები და ქვედა მიკროფონის პლატები",
            icon = "🔌",
            displayOrder = 9,
            isActive = true,
            isVisible = true,
            isPopular = false,
            popularOrder = 9,
            categoryType = "spare_part"
        ),
        CategoryItem(
            id = "cat_housings",
            name = "კორპუსები",
            slug = "housings",
            productType = "housing",
            localizedName = "Housings & Glass",
            description = "უკანა შუშები, ტიტანის ჩარჩოები და ღილაკების კომპლექტები",
            icon = "🛠️",
            displayOrder = 10,
            isActive = true,
            isVisible = true,
            isPopular = false,
            popularOrder = 10,
            categoryType = "spare_part"
        ),
        CategoryItem(
            id = "cat_accessories",
            name = "აქსესუარები",
            slug = "accessories",
            productType = "phone_accessory",
            localizedName = "Accessories",
            description = "დამცავი მინები, ქეისები და ყოველდღიური აქსესუარები",
            icon = "🎒",
            displayOrder = 11,
            isActive = true,
            isVisible = true,
            isPopular = false,
            popularOrder = 11,
            categoryType = "accessory"
        ),
        CategoryItem(
            id = "cat_keyboards",
            name = "კლავიატურები",
            slug = "keyboards",
            productType = "keyboard",
            localizedName = "Keyboards",
            description = "მექანიკური, უკაბელო და სამუშაო კლავიატურები",
            icon = "⌨️",
            displayOrder = 12,
            isActive = true,
            isVisible = true,
            isPopular = false,
            popularOrder = 12,
            categoryType = "accessory"
        ),
        CategoryItem(
            id = "cat_mice",
            name = "მაუსები",
            slug = "mice",
            productType = "mouse",
            localizedName = "Mice",
            description = "უკაბელო, გეიმინგ და ერგონომიული მაუსები",
            icon = "🖱️",
            displayOrder = 13,
            isActive = true,
            isVisible = true,
            isPopular = false,
            popularOrder = 13,
            categoryType = "accessory"
        ),
        CategoryItem(
            id = "cat_electronics",
            name = "ელექტრონიკა",
            slug = "electronics",
            productType = "other_electronics",
            localizedName = "Electronics",
            description = "სმარტ გაჯეტები და ტექნიკური ელექტრონიკა",
            icon = "🎛️",
            displayOrder = 14,
            isActive = true,
            isVisible = true,
            isPopular = false,
            popularOrder = 14,
            categoryType = "electronics"
        )
    )

    @Volatile
    private var cachedCategories: List<CategoryItem>? = null

    @Volatile
    private var cachedRawCategoriesJson: String? = null

    fun getCategories(context: Context): List<CategoryItem> {
        val prefs = getPrefs(context)
        val jsonStr = prefs.getString(KEY_CUSTOM_CATEGORIES, null)
        val policyVer = prefs.getInt(KEY_CATEGORIES_POLICY_VERSION, 0)
        val cached = cachedCategories
        if (cached != null && jsonStr == cachedRawCategoriesJson && policyVer >= CURRENT_CATEGORIES_POLICY_VERSION) {
            return cached.filterNot { ProductRepository.isProhibitedCategory(it) }
        }

        if (jsonStr.isNullOrBlank()) {
            val defaults = getDefaultCategories()
            cachedRawCategoriesJson = null
            cachedCategories = defaults
            return defaults
        }

        return try {
            val arr = JSONArray(jsonStr)
            val parsedList = mutableListOf<CategoryItem>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val rawName = obj.optString("name", "")
                val rawId = obj.optString("id", "")
                val rawProdType = obj.optString("productType", "")
                val normName = resolveCanonicalCategoryName(rawName, rawProdType, rawId)
                val meta = inferCategoryMetadata(normName)
                val canonicalId = if (rawId == "cat_audio" || meta.id.startsWith("cat_") && !meta.id.startsWith("cat_custom_")) {
                    meta.id
                } else {
                    rawId.ifBlank { meta.id }
                }
                val cat = CategoryItem(
                    id = canonicalId,
                    name = normName,
                    slug = obj.optString("slug", "").ifBlank { meta.slug },
                    productType = rawProdType.ifBlank { meta.productType },
                    localizedName = obj.optString("localizedName", "").ifBlank { meta.localizedName },
                    description = obj.optString("description", "").ifBlank { meta.description },
                    icon = obj.optString("icon", meta.icon).let {
                        if (normName == "აქსესუარები" && it == "🎧") "🎒" else it.ifBlank { meta.icon }
                    },
                    imageUrl = obj.optString("imageUrl", ""),
                    displayOrder = obj.optInt("displayOrder", i + 1),
                    isActive = obj.optBoolean("isActive", true),
                    isVisible = obj.optBoolean("isVisible", true),
                    isPopular = obj.optBoolean("isPopular", false),
                    popularOrder = obj.optInt("popularOrder", 0),
                    categoryType = obj.optString("categoryType", meta.categoryType),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                    updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                )
                if (cat.name.isNotBlank() && !ProductRepository.isProhibitedCategory(cat)) {
                    parsedList.add(cat)
                }
            }
            val finalList = if (parsedList.isEmpty()) {
                getDefaultCategories()
            } else {
                val deduped = parsedList
                    .distinctBy { it.id }
                    .distinctBy { it.name.lowercase() }
                    .toMutableList()
                if (policyVer < CURRENT_CATEGORIES_POLICY_VERSION) {
                    val existingNames = deduped.map { it.name.lowercase() }.toMutableSet()
                    val existingIds = deduped.map { it.id }.toMutableSet()
                    for (defCat in getDefaultCategories()) {
                        if (existingNames.add(defCat.name.lowercase()) && existingIds.add(defCat.id)) {
                            deduped.add(defCat)
                        }
                    }
                    prefs.edit().putInt(KEY_CATEGORIES_POLICY_VERSION, CURRENT_CATEGORIES_POLICY_VERSION).apply()
                    saveCategories(context, deduped, markUnsyncedLocalEdit = false)
                }
                deduped.sortedBy { it.displayOrder }
            }
            cachedRawCategoriesJson = prefs.getString(KEY_CUSTOM_CATEGORIES, jsonStr)
            cachedCategories = finalList
            finalList
        } catch (_: Exception) {
            val defaults = getDefaultCategories()
            cachedRawCategoriesJson = null
            cachedCategories = defaults
            defaults
        }
    }

    fun getCustomerVisibleCategories(context: Context): List<CategoryItem> {
        return getCategories(context)
            .filter { it.isActive && it.isVisible && !ProductRepository.isProhibitedCategory(it) }
            .sortedBy { it.displayOrder }
    }

    fun saveCategories(
        context: Context,
        categories: List<CategoryItem>,
        markUnsyncedLocalEdit: Boolean = true
    ) {
        val sanitized = categories
            .map { cat ->
                val norm = resolveCanonicalCategoryName(cat.name, cat.productType, cat.id)
                val meta = inferCategoryMetadata(norm)
                val resolvedId = if (cat.id == "cat_audio" || (meta.id.startsWith("cat_") && !meta.id.startsWith("cat_custom_"))) {
                    meta.id
                } else {
                    cat.id.ifBlank { meta.id }
                }
                cat.copy(
                    id = resolvedId,
                    name = norm,
                    slug = cat.slug.ifBlank { meta.slug },
                    productType = cat.productType.ifBlank { meta.productType }
                )
            }
            .filter { it.name.isNotBlank() && !ProductRepository.isProhibitedCategory(it) }
            .distinctBy { it.id }
            .distinctBy { it.name.lowercase() }
            .sortedBy { it.displayOrder }

        val arr = JSONArray()
        for (cat in sanitized) {
            val obj = JSONObject().apply {
                put("id", cat.id)
                put("name", cat.name)
                put("slug", cat.slug)
                put("productType", cat.productType)
                put("localizedName", cat.localizedName)
                put("description", cat.description)
                put("icon", cat.icon)
                put("imageUrl", cat.imageUrl)
                put("displayOrder", cat.displayOrder)
                put("isActive", cat.isActive)
                put("isVisible", cat.isVisible)
                put("isPopular", cat.isPopular)
                put("popularOrder", cat.popularOrder)
                put("categoryType", cat.categoryType)
                put("createdAt", cat.createdAt)
                put("updatedAt", cat.updatedAt)
            }
            arr.put(obj)
        }
        val serialized = arr.toString()
        cachedRawCategoriesJson = serialized
        cachedCategories = sanitized
        val editor = getPrefs(context).edit()
            .putString(KEY_CUSTOM_CATEGORIES, serialized)
            .putInt(KEY_CATEGORIES_POLICY_VERSION, CURRENT_CATEGORIES_POLICY_VERSION)
        if (markUnsyncedLocalEdit) {
            editor.putLong(KEY_LAST_LOCAL_EDIT_TS, System.currentTimeMillis())
                .putBoolean(KEY_HAS_UNSYNCED_LOCAL_EDITS, true)
        }
        editor.apply()
        categoriesVersion++
    }

    fun mergeCloudCategories(context: Context, cloudCategories: List<CategoryItem>): List<CategoryItem> {
        val cleanCloud = cloudCategories
            .map { raw ->
                val normName = resolveCanonicalCategoryName(raw.name, raw.productType, raw.id)
                val meta = inferCategoryMetadata(normName)
                val resolvedId = if (raw.id == "cat_audio" || (meta.id.startsWith("cat_") && !meta.id.startsWith("cat_custom_"))) {
                    meta.id
                } else {
                    raw.id.ifBlank { meta.id }
                }
                raw.copy(
                    id = resolvedId,
                    name = normName,
                    slug = raw.slug.ifBlank { meta.slug },
                    productType = raw.productType.ifBlank { meta.productType }
                )
            }
            .filter { it.name.isNotBlank() && !ProductRepository.isProhibitedCategory(it) }
        if (cleanCloud.isEmpty()) return getCategories(context)

        val currentLocal = getCategories(context)
        val mergedByName = LinkedHashMap<String, CategoryItem>()
        for (cloudCat in cleanCloud.sortedBy { it.displayOrder }) {
            mergedByName[cloudCat.name.lowercase()] = cloudCat
        }
        for (localCat in currentLocal) {
            val key = localCat.name.lowercase()
            if (!mergedByName.containsKey(key)) {
                mergedByName[key] = localCat
            }
        }
        val merged = mergedByName.values.sortedBy { it.displayOrder }
        saveCategories(context, merged, markUnsyncedLocalEdit = false)
        return merged
    }

    fun ensureCategoryExistsForProduct(
        context: Context,
        rawCategoryName: String,
        rawProductType: String = ""
    ): CategoryItem? {
        val normName = resolveCanonicalCategoryName(rawCategoryName, rawProductType)
        if (normName.isBlank()) return null
        val meta = inferCategoryMetadata(normName)
        val candidateCheck = CategoryItem(
            id = meta.id,
            name = normName,
            slug = meta.slug,
            productType = rawProductType.ifBlank { meta.productType },
            categoryType = meta.categoryType,
            description = meta.description
        )
        if (ProductRepository.isProhibitedCategory(candidateCheck)) return null

        val current = getCategories(context).toMutableList()
        val existingIdx = current.indexOfFirst {
            it.id == meta.id || it.name.equals(normName, ignoreCase = true)
        }
        if (existingIdx >= 0) {
            val existing = current[existingIdx]
            if (!existing.isActive || !existing.isVisible) {
                val reactivated = existing.copy(
                    isActive = true,
                    isVisible = true,
                    isPopular = true,
                    updatedAt = System.currentTimeMillis()
                )
                current[existingIdx] = reactivated
                saveCategories(context, current, markUnsyncedLocalEdit = true)
                return reactivated
            }
            return existing
        }

        val matchingDefault = getDefaultCategories().find {
            it.id == meta.id || it.name.equals(normName, ignoreCase = true)
        }
        val nextOrder = (current.maxOfOrNull { it.displayOrder } ?: 0) + 1
        val created = matchingDefault ?: CategoryItem(
            id = meta.id,
            name = normName,
            slug = meta.slug,
            productType = rawProductType.ifBlank { meta.productType },
            localizedName = meta.localizedName,
            description = meta.description,
            icon = meta.icon,
            displayOrder = nextOrder,
            isActive = true,
            isVisible = true,
            isPopular = true,
            popularOrder = nextOrder,
            categoryType = meta.categoryType,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        current.add(created)
        saveCategories(context, current, markUnsyncedLocalEdit = true)
        return created
    }

    fun addCategory(context: Context, category: CategoryItem) {
        val norm = resolveCanonicalCategoryName(category.name, category.productType, category.id)
        val meta = inferCategoryMetadata(norm)
        val normalizedCat = category.copy(
            id = if (category.id == "cat_audio") "cat_headphones" else category.id.ifBlank { meta.id },
            name = norm,
            slug = category.slug.ifBlank { meta.slug },
            productType = category.productType.ifBlank { meta.productType }
        )
        if (norm.isBlank() || ProductRepository.isProhibitedCategory(normalizedCat)) return
        val current = getCategories(context).toMutableList()
        val exists = current.any { it.id == normalizedCat.id || it.name.equals(normalizedCat.name, ignoreCase = true) }
        if (!exists) {
            current.add(normalizedCat)
            saveCategories(context, current)
        }
    }

    fun updateCategory(context: Context, category: CategoryItem) {
        val norm = normalizeCategoryName(category.name)
        val meta = inferCategoryMetadata(norm)
        val normalizedCat = category.copy(
            name = norm,
            slug = category.slug.ifBlank { meta.slug },
            productType = category.productType.ifBlank { meta.productType }
        )
        if (norm.isBlank() || ProductRepository.isProhibitedCategory(normalizedCat)) return
        val current = getCategories(context).toMutableList()
        val idx = current.indexOfFirst { it.id == normalizedCat.id }
        if (idx >= 0) {
            val oldName = current[idx].name
            current[idx] = normalizedCat.copy(updatedAt = System.currentTimeMillis())
            saveCategories(context, current)
            // თუ კატეგორიის სახელი შეიცვალა, განვაახლოთ მიბმული პროდუქტების კატეგორიის სახელიც (categoryId უცვლელია!)
            if (!oldName.equals(norm, ignoreCase = true)) {
                reassignProductsCategory(context, oldName, norm, normalizedCat.id)
            }
        }
    }

    fun getProductCountForCategory(context: Context, categoryName: String, categoryId: String = ""): Int {
        val clean = normalizeCategoryName(categoryName)
        val resolvedTargetId = resolveCanonicalCategoryId(clean, "", categoryId)
        if (clean.isBlank() && resolvedTargetId.isBlank()) return 0
        return loadProducts(context).count { prod ->
            val prodCatId = resolveCanonicalCategoryId(prod.category, prod.productType, prod.categoryId)
            prodCatId == resolvedTargetId || normalizeCategoryName(prod.category).equals(clean, ignoreCase = true)
        }
    }

    fun reassignProductsCategory(
        context: Context,
        oldCategory: String,
        newCategory: String,
        newCategoryId: String = ""
    ): Int {
        val cleanOld = normalizeCategoryName(oldCategory)
        val cleanNew = normalizeCategoryName(newCategory).ifBlank { "ელექტრონიკა" }
        val oldCatId = resolveCanonicalCategoryId(cleanOld)
        val targetCatId = newCategoryId.ifBlank { resolveCanonicalCategoryId(cleanNew) }
        if (cleanOld.isBlank()) return 0

        val products = loadProducts(context).toMutableList()
        var updatedCount = 0
        for (i in products.indices) {
            val prod = products[i]
            val prodCatId = resolveCanonicalCategoryId(prod.category, prod.productType, prod.categoryId)
            if (normalizeCategoryName(prod.category).equals(cleanOld, ignoreCase = true) || prodCatId == oldCatId) {
                if (prod.category != cleanNew || prod.categoryId != targetCatId) {
                    products[i] = prod.copy(category = cleanNew, categoryId = targetCatId)
                    updatedCount++
                }
            }
        }
        if (updatedCount > 0) {
            saveProducts(context, products, markUnsyncedLocalEdit = true)
        }
        return updatedCount
    }

    fun deleteCategory(
        context: Context,
        categoryId: String,
        reassignToCategoryName: String? = null
    ): Boolean {
        val current = getCategories(context).toMutableList()
        val target = current.find { it.id == categoryId } ?: return false

        // Reassign affected products if any
        val affectedCount = getProductCountForCategory(context, target.name, target.id)
        if (affectedCount > 0) {
            val destination = normalizeCategoryName(reassignToCategoryName ?: "").ifBlank { "ელექტრონიკა" }
            reassignProductsCategory(context, target.name, destination)
        }

        current.removeAll { it.id == categoryId }
        saveCategories(context, current)
        return true
    }

    // =========================================================================
    // დინამიური ბრენდების მართვა (Canonical Brand System + Normalization + CRUD)
    // =========================================================================
    private const val KEY_CUSTOM_BRANDS = "custom_brands_json_v1"

    var brandsVersion by mutableIntStateOf(0)
        private set

    @Volatile
    private var cachedBrands: List<BrandItem>? = null

    @Volatile
    private var cachedRawBrandsJson: String? = null

    /**
     * ბრენდის სახელის ნორმალიზაცია დუბლირების თავიდან ასაცილებლად:
     * აშორებს ზედმეტ ჰარებს, გადაყავს lowercase-ში და აერთიანებს ფონეტიკურ/სინონიმურ ვარიანტებს
     * (მაგ: "Hoco", "HOCO", "hoco ", "ჰოკო" -> "hoco").
     */
    fun normalizeBrandKey(raw: String): String {
        val clean = raw.trim().replace(Regex("\\s+"), " ").lowercase()
        if (clean.isBlank()) return ""
        return when (clean) {
            "hoco", "ჰოკო", "hoco." -> "hoco"
            "borofone", "ბოროფონი", "ბოროფონე" -> "borofone"
            "marshall", "მარშალი", "მარშალ" -> "marshall"
            "baseus", "ბასეუსი", "ბასეუს" -> "baseus"
            "anker", "ანკერი", "ანკერ" -> "anker"
            "ugreen", "უგრინი", "უგრინ", "u-green" -> "ugreen"
            "jbl", "ჯბლ", "ჯიბიელი" -> "jbl"
            "apple", "ეფლი", "ეფლ", "აიფონი" -> "apple"
            "samsung", "სამსუნგი", "სამსუნგ" -> "samsung"
            "xiaomi", "შაომი", "ქსიაომი", "redmi", "poco" -> "xiaomi"
            "google", "google pixel", "pixel", "გუგლი", "პიქსელი" -> "google pixel"
            "oneplus", "one plus", "ვანპლასი" -> "oneplus"
            "honor", "ჰონორი" -> "honor"
            "huawei", "ჰუავეი" -> "huawei"
            "tp-link", "tplink", "tp link" -> "tp-link"
            "zte", "ზტე" -> "zte"
            "sony", "სონი" -> "sony"
            "motorola", "მოტოროლა" -> "motorola"
            "realme", "რილმი" -> "realme"
            "logitech", "ლოჯიტეკი" -> "logitech"
            "razer", "რეიზერი" -> "razer"
            "hyperx", "ჰაიპერიქსი" -> "hyperx"
            "remax", "რემაქსი" -> "remax"
            "joyroom", "ჯოირუმი" -> "joyroom"
            "universal", "უნივერსალური" -> "universal"
            else -> clean
        }
    }

    fun formatCanonicalBrandDisplayName(raw: String): String {
        val trimmed = raw.trim().replace(Regex("\\s+"), " ")
        if (trimmed.isBlank()) return "Universal"
        return when (normalizeBrandKey(trimmed)) {
            "apple" -> "Apple"
            "samsung" -> "Samsung"
            "xiaomi" -> "Xiaomi"
            "hoco" -> "Hoco"
            "borofone" -> "Borofone"
            "marshall" -> "Marshall"
            "jbl" -> "JBL"
            "baseus" -> "Baseus"
            "anker" -> "Anker"
            "ugreen" -> "Ugreen"
            "google pixel" -> "Google Pixel"
            "oneplus" -> "OnePlus"
            "honor" -> "Honor"
            "huawei" -> "Huawei"
            "tp-link" -> "TP-Link"
            "zte" -> "ZTE"
            "sony" -> "Sony"
            "motorola" -> "Motorola"
            "realme" -> "Realme"
            "logitech" -> "Logitech"
            "razer" -> "Razer"
            "hyperx" -> "HyperX"
            "remax" -> "Remax"
            "joyroom" -> "Joyroom"
            "universal" -> "Universal"
            else -> {
                if (trimmed == trimmed.uppercase() && trimmed.length <= 4) {
                    trimmed
                } else {
                    trimmed.split(" ").joinToString(" ") { word ->
                        word.replaceFirstChar { ch ->
                            if (ch.isLowerCase()) ch.titlecase() else ch.toString()
                        }
                    }
                }
            }
        }
    }

    fun resolveCanonicalBrandId(rawBrandName: String, explicitBrandId: String = ""): String {
        val normKey = normalizeBrandKey(rawBrandName)
        if (normKey.isNotBlank()) {
            val slug = normKey.replace(Regex("[^a-z0-9ა-ჰ]+"), "_").trim('_')
            if (slug.isNotBlank()) return "brand_$slug"
        }
        val cleanExplicit = explicitBrandId.trim().lowercase()
        if (cleanExplicit.startsWith("brand_") && cleanExplicit.length > 6) {
            return cleanExplicit
        }
        return "brand_universal"
    }

    fun isSameBrand(brandA: String, brandB: String): Boolean {
        val keyA = normalizeBrandKey(brandA)
        val keyB = normalizeBrandKey(brandB)
        return keyA.isNotBlank() && keyA == keyB
    }

    fun getDefaultBrands(): List<BrandItem> {
        val orderedNames = listOf(
            "Apple",
            "Samsung",
            "Xiaomi",
            "Hoco",
            "Borofone",
            "Marshall",
            "JBL",
            "Baseus",
            "Anker",
            "Ugreen",
            "Google Pixel",
            "OnePlus",
            "Honor",
            "Huawei",
            "TP-Link",
            "ZTE",
            "Sony",
            "Motorola",
            "Realme"
        )
        return orderedNames.mapIndexed { index, displayName ->
            val norm = normalizeBrandKey(displayName)
            val slug = norm.replace(Regex("[^a-z0-9]+"), "_").trim('_')
            BrandItem(
                id = "brand_$slug",
                name = displayName,
                normalizedName = norm,
                slug = slug,
                logoUrl = "",
                isActive = true,
                sortOrder = index + 1
            )
        }
    }

    fun getBrands(context: Context): List<BrandItem> {
        val prefs = getPrefs(context)
        val jsonStr = prefs.getString(KEY_CUSTOM_BRANDS, null)
        val cached = cachedBrands
        if (cached != null && jsonStr == cachedRawBrandsJson) {
            return cached
        }

        val mergedByNorm = LinkedHashMap<String, BrandItem>()
        for (def in getDefaultBrands()) {
            mergedByNorm[def.normalizedName] = def
        }

        if (!jsonStr.isNullOrBlank()) {
            runCatching {
                val arr = JSONArray(jsonStr)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val rawName = obj.optString("name", "").trim()
                    if (rawName.isBlank()) continue
                    val displayName = formatCanonicalBrandDisplayName(rawName)
                    val norm = normalizeBrandKey(displayName)
                    if (norm.isBlank()) continue
                    val slug = obj.optString("slug", "").ifBlank {
                        norm.replace(Regex("[^a-z0-9ა-ჰ]+"), "_").trim('_')
                    }
                    val id = resolveCanonicalBrandId(displayName, obj.optString("id", ""))
                    val existing = mergedByNorm[norm]
                    val brandItem = BrandItem(
                        id = id,
                        name = displayName,
                        normalizedName = norm,
                        slug = slug,
                        logoUrl = obj.optString("logoUrl", existing?.logoUrl ?: ""),
                        isActive = obj.optBoolean("isActive", true),
                        sortOrder = obj.optInt("sortOrder", existing?.sortOrder ?: (mergedByNorm.size + 1)),
                        createdAt = obj.optLong("createdAt", existing?.createdAt ?: System.currentTimeMillis()),
                        updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                    )
                    mergedByNorm[norm] = brandItem
                }
            }
        }

        // ასევე ავტომატურად ჩავრთოთ აქტიური პროდუქტების ბრენდები, რომ არცერთი პროდუქტის ბრენდი არ დაიკარგოს
        runCatching {
            val products = cachedParsedProducts ?: ProductRepository.getAllProducts()
            for (p in products) {
                val rawBrand = p.brand.trim()
                if (rawBrand.isBlank()) continue
                val displayName = formatCanonicalBrandDisplayName(rawBrand)
                val norm = normalizeBrandKey(displayName)
                if (norm.isNotBlank() && !mergedByNorm.containsKey(norm)) {
                    val slug = norm.replace(Regex("[^a-z0-9ა-ჰ]+"), "_").trim('_')
                    mergedByNorm[norm] = BrandItem(
                        id = resolveCanonicalBrandId(displayName, p.brandId),
                        name = displayName,
                        normalizedName = norm,
                        slug = slug,
                        isActive = true,
                        sortOrder = mergedByNorm.size + 1
                    )
                }
            }
        }

        val finalList = mergedByNorm.values.sortedWith(compareBy({ it.sortOrder }, { it.name }))
        cachedRawBrandsJson = jsonStr
        cachedBrands = finalList
        return finalList
    }

    fun getCustomerVisibleBrands(context: Context): List<BrandItem> {
        return getBrands(context).filter { it.isActive }
    }

    fun saveBrands(context: Context, brands: List<BrandItem>) {
        val dedupedByNorm = LinkedHashMap<String, BrandItem>()
        for (b in brands) {
            val displayName = formatCanonicalBrandDisplayName(b.name)
            val norm = normalizeBrandKey(displayName)
            if (norm.isBlank()) continue
            val slug = b.slug.ifBlank { norm.replace(Regex("[^a-z0-9ა-ჰ]+"), "_").trim('_') }
            val canonicalId = resolveCanonicalBrandId(displayName, b.id)
            val normalizedItem = b.copy(
                id = canonicalId,
                name = displayName,
                normalizedName = norm,
                slug = slug
            )
            dedupedByNorm[norm] = normalizedItem
        }
        val sorted = dedupedByNorm.values.sortedWith(compareBy({ it.sortOrder }, { it.name }))
        val arr = JSONArray()
        for (item in sorted) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("name", item.name)
                put("normalizedName", item.normalizedName)
                put("slug", item.slug)
                put("logoUrl", item.logoUrl)
                put("isActive", item.isActive)
                put("sortOrder", item.sortOrder)
                put("createdAt", item.createdAt)
                put("updatedAt", item.updatedAt)
            }
            arr.put(obj)
        }
        val serialized = arr.toString()
        cachedRawBrandsJson = serialized
        cachedBrands = sorted
        getPrefs(context).edit()
            .putString(KEY_CUSTOM_BRANDS, serialized)
            .apply()
        brandsVersion++
    }

    fun mergeCloudBrands(context: Context, cloudBrands: List<BrandItem>): List<BrandItem> {
        if (cloudBrands.isEmpty()) return getBrands(context)
        val current = getBrands(context)
        val mergedByNorm = LinkedHashMap<String, BrandItem>()
        for (local in current) {
            mergedByNorm[local.normalizedName] = local
        }
        for (cloud in cloudBrands) {
            val displayName = formatCanonicalBrandDisplayName(cloud.name)
            val norm = normalizeBrandKey(displayName)
            if (norm.isBlank()) continue
            val slug = cloud.slug.ifBlank { norm.replace(Regex("[^a-z0-9ა-ჰ]+"), "_").trim('_') }
            val canonicalId = resolveCanonicalBrandId(displayName, cloud.id)
            mergedByNorm[norm] = cloud.copy(
                id = canonicalId,
                name = displayName,
                normalizedName = norm,
                slug = slug
            )
        }
        val finalBrands = mergedByNorm.values.sortedWith(compareBy({ it.sortOrder }, { it.name }))
        saveBrands(context, finalBrands)
        return finalBrands
    }

    /**
     * ამატებს ახალ ბრენდს ან აბრუნებს უკვე არსებულს (დუბლირების გარეშე).
     * მაგ: თუ "Hoco" უკვე არსებობს, "HOCO" ან "hoco" დააბრუნებს არსებულ "Hoco"-ს (second = false).
     */
    fun addOrGetBrand(context: Context, rawBrandName: String): Pair<BrandItem, Boolean> {
        val displayName = formatCanonicalBrandDisplayName(rawBrandName)
        val norm = normalizeBrandKey(displayName)
        val current = getBrands(context).toMutableList()
        val existingIdx = current.indexOfFirst { it.normalizedName == norm || isSameBrand(it.name, displayName) }
        if (existingIdx >= 0) {
            val existing = current[existingIdx]
            if (!existing.isActive) {
                val reactivated = existing.copy(isActive = true, updatedAt = System.currentTimeMillis())
                current[existingIdx] = reactivated
                saveBrands(context, current)
                return reactivated to false
            }
            return existing to false
        }
        val slug = norm.replace(Regex("[^a-z0-9ა-ჰ]+"), "_").trim('_').ifBlank { "custom" }
        val nextSortOrder = (current.maxOfOrNull { it.sortOrder } ?: 0) + 1
        val created = BrandItem(
            id = resolveCanonicalBrandId(displayName),
            name = displayName,
            normalizedName = norm,
            slug = slug,
            logoUrl = "",
            isActive = true,
            sortOrder = nextSortOrder,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        current.add(created)
        saveBrands(context, current)
        return created to true
    }

    fun ensureBrandExistsForProduct(context: Context, rawBrandName: String): BrandItem {
        return addOrGetBrand(context, rawBrandName.ifBlank { "Universal" }).first
    }

    // =========================================================================
    // ადმინისტრატორის სესიის მართვა (Admin Session Persistence & beqa1994@gmail.com)
    // =========================================================================
    fun isAdminSessionActive(context: Context): Boolean {
        if (!AuthAndOrderManager.isCurrentUserVerifiedSoleAdmin(context)) {
            clearAdminSession(context)
            return false
        }
        if (!AuthAndOrderManager.hasVerifiedAdminClaim(context)) {
            return false
        }
        val prefs = getPrefs(context)
        val isActive = prefs.getBoolean(KEY_ADMIN_SESSION_ACTIVE, false)
        val sessionEmail = prefs.getString(KEY_ADMIN_SESSION_EMAIL, null)
        if (!isActive || !AuthAndOrderManager.isSoleAdminEmail(sessionEmail)) {
            return false
        }
        val ts = prefs.getLong(KEY_ADMIN_SESSION_TS, 0L)
        val maxSessionDuration = java.util.concurrent.TimeUnit.DAYS.toMillis(7)
        if (System.currentTimeMillis() - ts > maxSessionDuration) {
            clearAdminSession(context)
            return false
        }
        return true
    }

    fun setAdminSessionActive(context: Context, active: Boolean, email: String? = null) {
        if (!AuthAndOrderManager.isCurrentUserVerifiedSoleAdmin(context)) {
            clearAdminSession(context)
            return
        }
        val rawActiveEmail = AuthAndOrderManager.resolveActiveUserEmail(context).trim()
        if (rawActiveEmail.isNotBlank() && !AuthAndOrderManager.isSoleAdminEmail(rawActiveEmail)) {
            clearAdminSession(context)
            return
        }
        val resolvedEmail = (email ?: rawActiveEmail.ifBlank { AuthAndOrderManager.SOLE_ADMIN_EMAIL }).trim()
        if (active && !AuthAndOrderManager.isSoleAdminEmail(resolvedEmail)) {
            clearAdminSession(context)
            return
        }
        val editor = getPrefs(context).edit()
            .putBoolean(KEY_ADMIN_SESSION_ACTIVE, active)
            .putLong(KEY_ADMIN_SESSION_TS, if (active) System.currentTimeMillis() else 0L)
        if (active && resolvedEmail.isNotBlank()) {
            editor.putString(KEY_ADMIN_SESSION_EMAIL, AuthAndOrderManager.SOLE_ADMIN_EMAIL)
        } else if (!active) {
            editor.remove(KEY_ADMIN_SESSION_EMAIL)
        }
        editor.commit()
    }

    fun clearAdminSession(context: Context) {
        getPrefs(context).edit()
            .putBoolean(KEY_ADMIN_SESSION_ACTIVE, false)
            .putLong(KEY_ADMIN_SESSION_TS, 0L)
            .remove(KEY_ADMIN_SESSION_EMAIL)
            .commit()
    }
}

