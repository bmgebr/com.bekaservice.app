package com.example

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * BEKASERVICE — ერთიანი ციფრული და პროგრამული სერვისების კატალოგი (24 სერვისი, 4 კატეგორია).
 * ყველა პროდუქტი ეფუძნება ოფიციალურ პროდუქტის მახასიათებლებს და მკაფიოდ მიჯნავს
 * ოფიციალურ გეგმასა და BEKASERVICE-ის მიწოდების/აქტივაციის პირობებს.
 */
object SoftwareCatalogData {

    const val SUBCAT_AI = "ხელოვნური ინტელექტის სერვისები"
    const val SUBCAT_GOOGLE = "Google-ის ანგარიშები და მეხსიერება"
    const val SUBCAT_CREATIVE = "ვიდეო, დიზაინი და კონტენტის შექმნა"
    const val SUBCAT_LICENSES = "პროგრამული უზრუნველყოფის ლიცენზიები"
    const val SUBCAT_MOBILE_UNLOCK = "მობილურის პროგრამირება და განბლოკვა"

    val softwareSubcategories = listOf(
        "ყველა",
        SUBCAT_AI,
        SUBCAT_GOOGLE,
        SUBCAT_CREATIVE,
        SUBCAT_LICENSES,
        SUBCAT_MOBILE_UNLOCK
    )

    fun isDigitalSoftwareService(product: ProductItem): Boolean {
        return product.id in 101..124 || product.shortDescription.isNotBlank()
    }

    fun getDigitalSoftwareProducts(): List<ProductItem> = cachedDigitalSoftwareProducts

    private val cachedDigitalSoftwareProducts: List<ProductItem> by lazy {
        listOf(
        // =====================================================================
        // A. ხელოვნური ინტელექტის სერვისები (11 პროდუქტი: ID 101 - 111)
        // =====================================================================
        ProductItem(
            id = 101,
            title = "ChatGPT Plus",
            price = "65 ₾",
            category = "პროგრამული სერვისები",
            type = "software",
            subcategory = SUBCAT_AI,
            imageUrl = "https://images.unsplash.com/photo-1677442136019-21780ecad995?w=800",
            shortDescription = "OpenAI-ის პრემიუმ გამოწერა: GPT-4o, o3-mini მსჯელობის მოდელები, DALL·E 3 სურათების გენერაცია, Advanced Voice და ფაილების ანალიზი.",
            description = "ChatGPT Plus არის OpenAI-ის ოფიციალური პრემიუმ სააბონენტო გეგმა ინდივიდუალური მომხმარებლებისთვის. უზრუნველყოფს წვდომას OpenAI-ის უახლეს მოდელებზე (GPT-4o, o3-mini), გაზრდილ შეტყობინებების ლიმიტებს პიკის საათებში, Advanced Voice ხმოვან რეჟიმს, DALL·E 3 გამოსახულების გენერაციას, Deep Research ფუნქციას, PDF/Excel ფაილების ანალიზსა და Custom GPTs ეკოსისტემას.",
            features = listOf(
                "წვდომა GPT-4o და o3-mini მსჯელობის (Reasoning) მოდელებზე",
                "Advanced Voice Mode — რეალურ დროში ბუნებრივი ხმოვანი დიალოგი",
                "DALL·E 3 სურათების გენერაცია და ფაილების/ცხრილების ღრმა ანალიზი",
                "Custom GPTs გამოყენება და საკუთარი ასისტენტების შექმნა",
                "ვებ-ძიება რეალურ დროში წყაროების მითითებით"
            ),
            targetAudience = "სტუდენტები, პროგრამისტები, მარკეტერები, მკვლევრები და კონტენტის შემქმნელები, რომლებსაც სჭირდებათ სწრაფი და საიმედო AI ასისტენტი ყოველდღიურ საქმიანობაში.",
            offerType = "თვიური გამოწერა (Subscription)",
            duration = "1 თვე (განახლებადი)",
            accountType = "პირადი ანგარიში (Private Upgrade) ან მზა ანგარიში — არჩევანით",
            activationTerms = "აქტივაცია სრულდება მომხმარებლის არჩევანით: მზა ვერიფიცირებულ ანგარიშზე ან თქვენს ანგარიშზე ოფიციალური გამოწერის გააქტიურებით. უსაფრთხოების წესი: არასდროს გაუზიაროთ არავის საბანკო ბარათის კოდები.",
            officialUrl = "https://openai.com/chatgpt/pricing/",
            termsUrl = "https://openai.com/policies/terms-of-use/",
            lastVerifiedAt = "2026-10-25",
            searchKeywords = "chatgpt plus ჩატჯიპიტი პლუსი ჯიპიტი openai ოპენაი gpt4 gpt-4o ai ხელოვნური ინტელექტი",
            isFeatured = true,
            displayOrder = 2,
            brand = "OpenAI",
            compatibleModels = "Web, iOS, Android, macOS, Windows",
            warranty = "აქტიური გამოწერის სრული პერიოდი",
            brandColorHex = "#10A37F",
            brandBadgeText = "OpenAI • Plus"
        ),
        ProductItem(
            id = 102,
            title = "ChatGPT Pro",
            price = "ფასი შეთანხმებით",
            category = "პროგრამული სერვისები",
            type = "software",
            subcategory = SUBCAT_AI,
            imageUrl = "https://images.unsplash.com/photo-1620712943543-bcc4688e7485?w=800",
            shortDescription = "OpenAI-ის უმაღლესი დონის გეგმა მკვლევრებისა და ინჟინრებისთვის: o1/o3 Pro Mode, გაფართოებული Deep Research და მაქსიმალური გამოთვლითი რესურსი.",
            description = "ChatGPT Pro წარმოადგენს OpenAI-ის ფლაგმანურ სააბონენტო პაკეტს პროფესიონალებისთვის, რომლებსაც სჭირდებათ მაქსიმალური გამოთვლითი სიმძლავრე ურთულესი პროგრამული, მათემატიკური, სამეცნიერო და ანალიტიკური ამოცანების გადასაჭრელად. მოიცავს Pro Mode მსჯელობას, გაფართოებულ Deep Research კვლევასა და პრიორიტეტულ სისწრაფეს.",
            features = listOf(
                "უმაღლესი დონის მსჯელობის მოდელები (Pro Reasoning Mode)",
                "გაფართოებული წვდომა Deep Research და Sora ვიდეო გენერაციაზე",
                "მაქსიმალური კონტექსტური ფანჯარა და პრიორიტეტული გამოთვლითი რესურსი",
                "რთული კודის არქიტექტურის, მონაცემთა მეცნიერებისა და კვლევების ანალიზი"
            ),
            targetAudience = "Senior დეველოპერები, AI ინჟინრები, მონაცემთა ანალიტიკოსები და სამეცნიერო მკვლევრები.",
            offerType = "პროფესიონალური თვიური გეგმა (Pro Tier)",
            duration = "1 თვე (ინფორმაცია გადასამოწმებელია შეკვეთისას)",
            accountType = "პირადი ანგარიში (Private Account)",
            activationTerms = "პაკეტის ღირებულება და აქტივაციის ფორმატი დამოკიდებულია მიმდინარე ხელმისაწვდომობაზე. დეტალები ზუსტდება უშუალოდ შეკვეთის გაფორმებისას BEKASERVICE-ის ოპერატორთან.",
            officialUrl = "https://openai.com/chatgpt/pricing/",
            termsUrl = "https://openai.com/policies/terms-of-use/",
            lastVerifiedAt = "2026-10-25",
            searchKeywords = "chatgpt pro ჩატჯიპიტი პრო ჯიპიტი პრო openai pro o1 pro o3 pro deep research",
            isFeatured = false,
            displayOrder = 4,
            brand = "OpenAI",
            compatibleModels = "Web, iOS, Android, macOS, Windows",
            warranty = "აქტიური გამოწერის სრული პერიოდი",
            brandColorHex = "#10A37F",
            brandBadgeText = "OpenAI • Pro"
        ),
        ProductItem(
            id = 103,
            title = "Google AI Pro",
            price = "95 ₾",
            category = "პროგრამული სერვისები",
            type = "software",
            subcategory = SUBCAT_AI,
            imageUrl = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=800",
            shortDescription = "Google-ის პრემიუმ AI პაკეტი (Gemini Advanced / 2.5 Pro + 2TB Google One Cloud + NotebookLM Plus) — ხელმისაწვდომია 18-თვიანი სპეც-შეთავაზებით.",
            description = "Google AI Pro (Google One AI Premium ეკოსისტემა) აერთიანებს Google-ის ყველაზე მძლავრ Gemini 2.5 Pro / Advanced მოდელებს, Deep Research-ს, NotebookLM Plus-ს, Gemini-ის ინტეგრაციას Gmail/Docs/Sheets-ში და 2TB Google One ღრუბლოვან მეხსიერებას.\n\nმნიშვნელოვანი განმარტება: ოფიციალურად Google AI Pro წარმოადგენს ყოველთვიურ/წლიურ სააბონენტო გეგმას, ხოლო BEKASERVICE-ის სპეციალური პრომო-შეთავაზების ფარგლებში ხელმისაწვდომია 18-თვიანი აქტივაციის პაკეტი.",
            features = listOf(
                "Gemini Advanced (Gemini 2.5 Pro) — 1M+ ტოკენიანი კონტექსტური მეხსიერებით",
                "2 TB (2000 GB) ოფიციალური Google One მეხსიერება (Drive, Gmail, Photos)",
                "Gemini ინტეგრაცია პირდაპირ Gmail, Google Docs, Sheets და Slides-ში",
                "NotebookLM Plus — გაზრდილი აუდიო მიმოხილვებითა და წყაროების ლიმიტით",
                "18-თვიანი სპეციალური აქციის პაკეტი BEKASERVICE-ში (ასევე ხელმისაწვდომია სტანდარტული ვადით)"
            ),
            targetAudience = "მომხმარებლები, რომლებსაც სურთ ერთდროულად უმაღლესი დონის AI ასისტენტი (Gemini Pro) და 2TB Google-ის ღრუბლოვანი საცავი ფოტოებისა და ფაილებისთვის.",
            offerType = "სპეციალური 18-თვიანი აქციის პაკეტი / Google One AI Premium",
            duration = "18 თვე (სპეც-შეთავაზება)",
            accountType = "პირადი ანგარიში (Private Account / Invite ან ინდივიდუალური აქტივაცია)",
            activationTerms = "ოფიციალური პროდუქტი მოიცავს Gemini Advanced + 2TB მეხსიერებას. 18-თვიანი ხანგრძლივობა წარმოადგენს BEKASERVICE-ის სპეციალურ პრომო-პაკეტს. აქტივაცია ხდება უსაფრთხოდ, პირადი მონაცემების სრული დაცვით.",
            officialUrl = "https://one.google.com/about/ai-premium/",
            termsUrl = "https://one.google.com/terms-of-service",
            lastVerifiedAt = "2026-10-25",
            searchKeywords = "google ai pro გუგლ აი პრო gemini advanced ჯემინი პრო 18 თვე 18-month google one 2tb",
            isFeatured = true,
            displayOrder = 1,
            brand = "Google",
            compatibleModels = "Android, iOS, Web, Google Workspace, Chromebook",
            warranty = "აქტივაციის სრული გარანტია",
            brandColorHex = "#4285F4",
            brandBadgeText = "Google AI • 18 თვე"
        ),
        ProductItem(
            id = 104,
            title = "Google AI Ultra",
            price = "ფასი შეთანხმებით",
            category = "პროგრამული სერვისები",
            type = "software",
            subcategory = SUBCAT_AI,
            imageUrl = "https://images.unsplash.com/photo-1634017839464-5c339ebe3cb4?w=800",
            shortDescription = "Google-ის უმაღლესი ტექნოლოგიური პაკეტი: Gemini Ultra / Deep Think, Veo ვიდეო გენერაციის გაზრდილი ლიმიტები და YouTube/Cloud პრემიუმ ბენეფიტები.",
            description = "Google AI Ultra არის Google-ის ფლაგმანური AI გამოწერა პროფესიონალი შემქმნელებისთვის, დეველოპერებისა და კreative სტუდიებისთვის. უზრუნველყოფს უმაღლეს ლიმიტებს Gemini-ის ღრმა აზროვნების მოდელებზე, Veo ვიდეო გენერაციის ინსტრუმენტებზე (Flow), Project Mariner-ის ადრეულ წვდომასა და გაფართოებულ Google One ღრუბლოვან საცავზე.",
            features = listOf(
                "Google-ის ყველაზე მძლავრი AI მოდელებისა და Deep Think რეჟიმის მაქსიმალური ლიმიტები",
                "Veo ვიდეო გენერაცია და კინემატოგრაფიული AI ინსტრუმენტები (Flow)",
                "NotebookLM-ის უმაღლესი ლიმიტები და გაფართოებული Google Cloud მეხსიერება",
                "პრიორიტეტული წვდომა Google-ის უახლეს ექსპერიმენტულ AI პროექტებზე"
            ),
            targetAudience = "ვიდეო პროდიუსერები, AI მკვლევრები, დეველოპერები და კომპანიები, რომლებსაც სჭირდებათ Google-ის მაქსიმალური AI რესურსი.",
            offerType = "უმაღლესი დონის სააბონენტო გეგმა (Ultra Tier)",
            duration = "1 თვე (ინფორმაცია გადასამოწმებელია შეკვეთისას)",
            accountType = "პირადი ანგარიში (Private Account)",
            activationTerms = "ფასი და რეგიონული ხელმისაწვდომობის პირობები მოწმდება შეკვეთის მომენტში BEKASERVICE-ის კონსულტანტთან.",
            officialUrl = "https://one.google.com/about/plans",
            termsUrl = "https://one.google.com/terms-of-service",
            lastVerifiedAt = "2026-10-25",
            searchKeywords = "google ai ultra გუგლ აი ულტრა gemini ultra ჯემინი ულტრა veo google one ultra",
            isFeatured = false,
            displayOrder = 5,
            brand = "Google",
            compatibleModels = "Web, Android, iOS, Google Workspace",
            warranty = "აქტიური გამოწერის სრული პერიოდი",
            brandColorHex = "#8AB4F8",
            brandBadgeText = "Google • AI Ultra"
        ),
        ProductItem(
            id = 105,
            title = "Grok SuperGrok",
            price = "85 ₾",
            category = "პროგრამული სერვისები",
            type = "software",
            subcategory = SUBCAT_AI,
            imageUrl = "https://images.unsplash.com/photo-1635070041078-e363dbe005cb?w=800",
            shortDescription = "xAI-ის პრემიუმ გამოწერა: Grok 3 მოდელი, DeepSearch რეალურ დროში, Think მსჯელობის რეჟიმი და Aurora სურათების გენერაცია.",
            description = "Grok SuperGrok არის xAI-ის ოფიციალური გაფართოებული გამოწერა grok.com-სა და Grok მობილურ აპლიკაციაში. მომხმარებელს აძლევს გაზრდილ კვოტებს Grok 3 მოდელზე, DeepSearch სიღრმისეულ ინტერნეტ-კვლევაზე, Think (მსჯელობის) რეჟიმსა და Aurora ფოტორეალისტური სურათების გენერირებაზე.",
            features = listOf(
                "გაზრდილი ლიმიტები Grok 3 და Think (Reasoning) რეჟიმებზე",
                "DeepSearch — რეალურ დროში ვებისა და X პლატფორმის ღრმა ანალიზი",
                "Aurora მოდელით მაღალი ხარისხის გამოსახულებების გენერაცია",
                "მუშაობს grok.com ვებ-პლატფორმაზე, iOS და Android აპლიკაციებში"
            ),
            targetAudience = "ანალიტიკოსები, ტრეიდერები, ჟურნალისტები, დეველოპერები და მომხმარებლები, რომლებსაც სჭირდებათ რეალურ დროში განახლებადი ინფორმაცია.",
            offerType = "თვიური გამოწერა (SuperGrok Subscription)",
            duration = "1 თვე",
            accountType = "პირადი ანგარიში (Private Account)",
            activationTerms = "აქტივაცია სრულდება grok.com-ის ინდივიდუალურ ანგარიშზე. დეტალები და მიწოდების დრო ზუსტდება შეკვეთისას.",
            officialUrl = "https://grok.com/",
            termsUrl = "https://x.ai/legal/terms-of-service",
            lastVerifiedAt = "2026-10-25",
            searchKeywords = "grok supergrok გროკი სუპერგროკი xai ილონ მასკი grok 3 deepsearch",
            isFeatured = false,
            displayOrder = 6,
            brand = "xAI",
            compatibleModels = "Web (grok.com), iOS, Android",
            warranty = "აქტიური გამოწერის სრული პერიოდი",
            brandColorHex = "#E2E8F0",
            brandBadgeText = "xAI • SuperGrok"
        ),
        ProductItem(
            id = 106,
            title = "Grok SuperGrok Heavy",
            price = "ფასი შეთანხმებით",
            category = "პროგრამული სერვისები",
            type = "software",
            subcategory = SUBCAT_AI,
            imageUrl = "https://images.unsplash.com/photo-1526374965328-7f61d4dc18c5?w=800",
            shortDescription = "xAI-ის მაქსიმალური სიმძლავრის პაკეტი: მრავალაგენტიანი ღრმა ანალიზი, უმაღლესი კვოტები და ადრეული წვდომა xAI-ის ფლაგმანურ მოდელებზე.",
            description = "Grok SuperGrok Heavy განკუთვნილია ინტენსიური პროფესიონალური დატვირთვისთვის. მოიცავს xAI-ის ყველაზე მძლავრ მრავალაგენტიან მსჯელობის რეჟიმს (Heavy Reasoning), მაქსიმალურ კონტექსტურ ფანჯარასა და პრიორიტეტულ გამოთვლით სიმძლავრეს.",
            features = listOf(
                "SuperGrok Heavy მრავალაგენტიანი პარალელური მსჯელობის რეჟიმი",
                "უმაღლესი ლიმიტები DeepSearch და კოდის გენერაციის ამოცანებზე",
                "პრიორიტეტული წვდომა xAI-ის ახალ მოდელებსა და ფუნქციებზე"
            ),
            targetAudience = "AI ინჟინრები, ფინანსური და ტექნიკური ანალიტიკოსები, კვლევითი ჯგუფები.",
            offerType = "პროფესიონალური Heavy გამოწერა",
            duration = "1 თვე (ინფორმაცია გადასამოწმებელია)",
            accountType = "პირადი ანგარიში (Private Account)",
            activationTerms = "ფასი და ხელმისაწვდომობა მოწმდება ინდივიდუალურად შეკვეთის გაფორმებისას.",
            officialUrl = "https://grok.com/",
            termsUrl = "https://x.ai/legal/terms-of-service",
            lastVerifiedAt = "2026-10-25",
            searchKeywords = "grok supergrok heavy გროკ ჰევი სუპერგროკ ჰევი xai heavy",
            isFeatured = false,
            displayOrder = 7,
            brand = "xAI",
            compatibleModels = "Web (grok.com), iOS, Android",
            warranty = "აქტიური გამოწერის სრული პერიოდი",
            brandColorHex = "#94A3B8",
            brandBadgeText = "xAI • Heavy"
        ),
        ProductItem(
            id = 107,
            title = "Claude Pro — Private Account",
            price = "70 ₾",
            category = "პროგრამული სერვისები",
            type = "software",
            subcategory = SUBCAT_AI,
            imageUrl = "https://images.unsplash.com/photo-1555949963-ff9fe0c870eb?w=800",
            shortDescription = "Anthropic-ის ოფიციალური Claude Pro ინდივიდუალურ (Private) ანგარიშზე — სრული კონფიდენციალურობა, Claude 3.7 Sonnet, Projects და Artifacts.",
            description = "Claude Pro — Private Account გაძლევთ სრულ, ინდივიდუალურ წვდომას Anthropic-ის პრემიუმ მოდელებზე (Claude 3.7 Sonnet, Extended Thinking, Claude 3.5 Haiku). Private ანგარიშის უპირატესობაა ის, რომ ანგარიშს იყენებთ მხოლოდ თქვენ: თქვენი მიმოწერა, ატვირთული დოკუმენტები, კოდის პროექტები (Projects) და შეტყობინებების ლიმიტი სრულად დაცული და იზოლირებულია.",
            features = listOf(
                "100% ინდივიდუალური (Private) ანგარიში — პირადი ჩატები და ფაილები მხოლოდ თქვენთვის",
                "Claude 3.7 Sonnet (Extended Thinking) — საუკეთესო პროგრამირებისა და ტექსტის ანალიზისთვის",
                "Projects & Knowledge Base — დიდი დოკუმენტებისა და კოდის ბაზის ატვირთვა",
                "Artifacts — ინტერაქტიული კოდის, დიაგრამებისა და UI კომპონენტების ვიზუალიზაცია",
                "უფასო ვერსიასთან შედარებით 5-ჯერ მეტი გამოყენების კვოტა"
            ),
            targetAudience = "პროგრამისტები, მწერლები, იურისტები და მკვლევრები, რომლებისთვისაც პრიორიტეტულია მონაცემთა კონფიდენციალურობა და პირადი ლიმიტები.",
            offerType = "თვიური გამოწერა (Private Account)",
            duration = "1 თვე",
            accountType = "პირადი ანგარიში (Private — მხოლოდ 1 მომხმარებლისთვის)",
            activationTerms = "გადმოგეცემათ ინდივიდუალური ვერიფიცირებული Claude Pro ანგარიში ან გააქტიურდება თქვენს ანგარიშზე. სრული კვოტა გეკუთვნით მხოლოდ თქვენ.",
            officialUrl = "https://www.anthropic.com/pricing",
            termsUrl = "https://www.anthropic.com/legal/consumer-terms",
            lastVerifiedAt = "2026-10-25",
            searchKeywords = "claude pro private კლოდ პრო კლოდი anthropic ანთროპიკი sonnet სონეტი პირადი ანგარიში",
            isFeatured = false,
            displayOrder = 8,
            brand = "Anthropic",
            compatibleModels = "Web (claude.ai), iOS, Android, macOS, Windows",
            warranty = "1 თვე სრული გარანტია",
            brandColorHex = "#D97757",
            brandBadgeText = "Claude • Private"
        ),
        ProductItem(
            id = 108,
            title = "Claude Pro — Shared Account",
            price = "35 ₾",
            category = "პროგრამული სერვისები",
            type = "software",
            subcategory = SUBCAT_AI,
            imageUrl = "https://images.unsplash.com/photo-1516321318423-f06f85e504b3?w=800",
            shortDescription = "Claude Pro-ს ბიუჯეტური გაზიარებული (Shared) ანგარიში — წვდომა Claude 3.7 Sonnet-სა და Artifacts-ზე ხელმისაწვდომ ფასად.",
            description = "Claude Pro — Shared Account არის ეკონომიური ალტერნატივა მათთვის, ვისაც სურს Anthropic-ის ფლაგმანური Claude 3.7 Sonnet მოდელის გამოყენება მინიმალური დანახარჯით.\n\nმნიშვნელოვანი განსხვავება Private ანგარიშისგან: Shared ანგარიშს იყენებს რამდენიმე მომხმარებელი, ამიტომ შეტყობინებების დღიური ლიმიტი და სამუშაო სივრცე გაზიარებულია. კონფიდენციალური დოკუმენტებისთვის რეკომენდებულია Private Account-ის არჩევა.",
            features = listOf(
                "ხელმისაწვდომი ფასი Claude Pro-ს ყველა ძირითად მოდელზე წვდომისთვის",
                "Claude 3.7 Sonnet და Extended Thinking მხარდაჭერა",
                "Artifacts და კოდის გენერაციის სრული ფუნქციონალი",
                "მკაფიოდ განსაზღვრული პირობა: გაზიარებული კვოტა და საერთო სამუშაო გარემო"
            ),
            targetAudience = "სტუდენტები და მომხმარებლები, რომლებსაც სურთ Claude Pro-ს გამოცდა ბიუჯეტურ ფასად არაკონფიდენციალურ ამოცანებზე.",
            offerType = "თვიური ბიუჯეტური წვდომა (Shared Access)",
            duration = "1 თვე",
            accountType = "გაზიარებული ანგარიში (Shared — რამდენიმე მომხმარებელზე)",
            activationTerms = "ანგარიშის პაროლის ან პარამეტრების შეცვლა აკრძალულია. კონფიდენციალური/პერსონალური ფაილების ატვირთვა არ არის რეკომენდებული.",
            officialUrl = "https://www.anthropic.com/pricing",
            termsUrl = "https://www.anthropic.com/legal/consumer-terms",
            lastVerifiedAt = "2026-10-25",
            searchKeywords = "claude pro shared კლოდ პრო გაზიარებული იაფი anthropic sonnet",
            isFeatured = false,
            displayOrder = 9,
            brand = "Anthropic",
            compatibleModels = "Web (claude.ai), iOS, Android",
            warranty = "1 თვე სრული გარანტია",
            brandColorHex = "#D97757",
            brandBadgeText = "Claude • Shared"
        ),
        ProductItem(
            id = 109,
            title = "Perplexity Pro",
            price = "50 ₾",
            category = "პროგრამული სერვისები",
            type = "software",
            subcategory = SUBCAT_AI,
            imageUrl = "https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=800",
            shortDescription = "AI საძიებო და კვლევითი პლატფორმა: 300+ Pro Search დღეში, Deep Research და მოდელების არჩევანი (GPT-4o, Claude 3.7 Sonnet, Sonar, Grok).",
            description = "Perplexity Pro არის მსოფლიოს წამყვანი AI საძიებო და კვლევითი სისტემა, რომელიც ყველა პასუხს ამყარებს გადამოწმებული წყაროებითა და ციტირებებით. Pro გეგმა გაძლევთ საშუალებას ერთ სივრცეში გადართოთ წამყვანი AI მოდელები (Claude 3.7 Sonnet, GPT-4o, Gemini 2.5 Pro, Sonar Large), ატვირთოთ შეუზღუდავი ფაილები და ჩაატაროთ ღრმა აკადემიური თუ ბიზნეს კვლევები.",
            features = listOf(
                "დღეში 300+ Pro Search და Deep Research ანალიტიკური ანგარიშები",
                "წამყვანი მოდელების არჩევანი ერთ ანგარიშში: GPT-4o, Claude 3.7, Gemini Pro, Sonar",
                "ყველა პასუხი დამოწმებული ბმულებითა და პირველწყაროებით",
                "PDF, CSV და დოკუმენტების ატვირთვა და შეჯამება (Spaces)"
            ),
            targetAudience = "მკვლევრები, სტუდენტები, ჟურნალისტები, მარკეტერები და დეველოპერები, რომლებსაც სჭირდებათ ფაქტებზე დაფუძნებული პასუხები.",
            offerType = "თვიური ან წლიური გამოწერა",
            duration = "1 თვე (ხელმისაწვდომია 12-თვიანი ვარიანტიც შეთანხმებით)",
            accountType = "პირადი ანგარიში (Private Account)",
            activationTerms = "აქტივაცია სრულდება ინდივიდუალურ ანგარიშზე სრული Pro ფუნქციონალით.",
            officialUrl = "https://www.perplexity.ai/pro",
            termsUrl = "https://www.perplexity.ai/hub/legal/terms-of-service",
            lastVerifiedAt = "2026-10-25",
            searchKeywords = "perplexity pro პერპლექსიტი პრო ai search კვლევა ძიება",
            isFeatured = false,
            displayOrder = 10,
            brand = "Perplexity",
            compatibleModels = "Web, iOS, Android, macOS",
            warranty = "აქტიური გამოწერის სრული პერიოდი",
            brandColorHex = "#20B8CD",
            brandBadgeText = "Perplexity • Pro"
        ),
        ProductItem(
            id = 110,
            title = "Cursor AI Pro",
            price = "70 ₾",
            category = "პროგრამული სერვისები",
            type = "software",
            subcategory = SUBCAT_AI,
            imageUrl = "https://images.unsplash.com/photo-1555066931-4365d14bab8c?w=800",
            shortDescription = "პროგრამისტების #1 AI კოდის რედაქტორი (IDE): 500 სწრაფი პრემიუმ მოთხოვნა, Agent Mode, Claude 3.7 Sonnet & GPT-4o ინტეგრაცია და Cursor Tab.",
            description = "Cursor AI Pro არის VS Code-ზე დაფუძნებული თანამედროვე AI კოდის რედაქტორი, რომელიც სრულად კითხულობს თქვენს პროექტს (Codebase Indexing). Pro გეგმა მოიცავს 500 Fast Premium Request-ს თვეში (Claude 3.7 Sonnet, GPT-4o, o3-mini), შეუზღუდავ Cursor Tab ავტო-დასრულებას და მრავალფაილიან Agent / Composer რეჟიმს.",
            features = listOf(
                "500 Fast Premium Request თვეში + შეუზღუდავი Slow Requests პრემიუმ მოდელებზე",
                "შეუზღუდავი Cursor Tab — ჭკვიანი მრავალხაზიანი კოდის ავტო-შევსება",
                "Agent & Composer Mode — მთლიანი ფუნქციებისა და ფაილების ავტომატური დაწერა/რეფაქტორინგი",
                "მთლიანი რეპოზიტორიის კონტექსტის გააზრება (@Codebase)"
            ),
            targetAudience = "Web, Mobile (Android/iOS), Backend და Full-Stack პროგრამისტები.",
            offerType = "თვიური გამოწერა (Cursor Pro Plan)",
            duration = "1 თვე",
            accountType = "პირადი ანგარიში (Private Account)",
            activationTerms = "აქტივაცია სრულდება ინდივიდუალურ ანგარიშზე. მუშაობს Windows, macOS და Linux სისტემებზე.",
            officialUrl = "https://www.cursor.com/pricing",
            termsUrl = "https://www.cursor.com/terms-of-service",
            lastVerifiedAt = "2026-10-25",
            searchKeywords = "cursor ai pro კურსორი კურსორ აი პრო ide კოდინგი პროგრამირება vs code",
            isFeatured = false,
            displayOrder = 11,
            brand = "Cursor",
            compatibleModels = "Windows, macOS, Linux",
            warranty = "1 თვე სრული გარანტია",
            brandColorHex = "#38BDF8",
            brandBadgeText = "Cursor • AI IDE"
        ),
        ProductItem(
            id = 111,
            title = "Lovable Pro",
            price = "80 ₾",
            category = "პროგრამული სერვისები",
            type = "software",
            subcategory = SUBCAT_AI,
            imageUrl = "https://images.unsplash.com/photo-1460925895917-afdab827c52f?w=800",
            shortDescription = "AI Full-Stack ვებ-აპლიკაციების შემქმნელი პლატფორმა: კერძო პროექტები, GitHub და Supabase ინტეგრაცია, საკუთარი დომენის მიბმა.",
            description = "Lovable Pro (lovable.dev) გაძლევთ საშუალებას ბუნებრივი ენის (Prompt-ების) საშუალებით შექმნათ სრულფასოვანი Web აპლიკაციები React, Tailwind, Supabase და GitHub სინქრონიზაციით. Pro გეგმა ხსნის კერძო პროექტების (Private Projects) შექმნის შესაძლებლობას, გაზრდილ თვიურ კრედიტებს, კოდის პირდაპირ რედაქტირებასა და Custom Domain-ის მიბმას.",
            features = listOf(
                "გაზრდილი თვიური შეტყობინებების/კრედიტების ლიმიტი აპლიკაციების ასაწყობად",
                "Private Projects — დახურული კომერციული პროექტების შექმნა",
                "ორმხრივი GitHub სინქრონიზაცია და Supabase ბაზის პირდაპირი ინტეგრაცია",
                "საკუთარი დომენის (Custom Domain) მიბმა და Lovable ბეიჯის მოხსნა"
            ),
            targetAudience = "სტარტაპერები, პროდუქტის მენეჯერები, დიზაინერები და დეველოპერები სწრაფი MVP-ებისა და ვებ-პლატფორმების შესაქმნელად.",
            offerType = "თვიური გამოწერა (Lovable Pro)",
            duration = "1 თვე",
            accountType = "პირადი ანგარიში (Private Account)",
            activationTerms = "აქტივაციის დეტალები და კრედიტების პაკეტი ზუსტდება შეკვეთის გაფორმებისას.",
            officialUrl = "https://lovable.dev/pricing",
            termsUrl = "https://lovable.dev/terms",
            lastVerifiedAt = "2026-10-25",
            searchKeywords = "lovable pro ლავაბლ პრო lovable dev ვებ აპლიკაციის შექმნა no code ai builder",
            isFeatured = false,
            displayOrder = 12,
            brand = "Lovable",
            compatibleModels = "Web Browser (Desktop & Mobile)",
            warranty = "აქტიური გამოწერის სრული პერიოდი",
            brandColorHex = "#EC4899",
            brandBadgeText = "Lovable • Pro"
        ),

        // =====================================================================
        // B. Google-ის ანგარიშები და მეხსიერება (4 პროდუქტი: ID 112 - 115)
        // =====================================================================
        ProductItem(
            id = 112,
            title = "Gmail Phone Verified Account",
            price = "15 ₾",
            category = "პროგრამული სერვისები",
            type = "software",
            subcategory = SUBCAT_GOOGLE,
            imageUrl = "https://images.unsplash.com/photo-1557200134-90327ee9fafa?w=800",
            shortDescription = "სუფთა, ტელეფონის ნომრით ვერიფიცირებული ახალი Google / Gmail ანგარიში — სრული წვდომითა და უსაფრთხო გადაბარებით.",
            description = "ახალი, სუფთა ისტორიის მქონე Google (Gmail) ანგარიში, რომელიც ვერიფიცირებულია ტელეფონის ნომრით და მზად არის ყველა Google სერვისში (Gmail, YouTube, Google Drive, Play Store, Google Ads/Business) გამოსაყენებლად.\n\nუსაფრთხოების პოლიტიკა: ანგარიშის მიღებისთანავე მომხმარებელს ეძლევა სრული ინსტრუქცია პაროლის, აღდგენის ელფოსტისა (Recovery Email) და საკუთარი 2FA დაცვის დასაყენებლად.",
            features = listOf(
                "ტელეფონის ნომრით ვერიფიცირებული სუფთა Google ანგარიში",
                "სრული თავსებადობა Gmail, Drive, YouTube, Play Store და სხვა სერვისებთან",
                "პაროლისა და Recovery მონაცემების სრული შეცვლის შესაძლებლობა",
                "მყისიერი მიწოდება უსაფრთხოების ინსტრუქციასთან ერთად"
            ),
            targetAudience = "ბიზნეს მომხმარებლები, ციფრული მარკეტერები და პირები, რომლებსაც სჭირდებათ დამატებითი ვერიფიცირებული Google ანგარიში.",
            offerType = "ერთჯერადი შენაძენი (ახალი ანგარიში)",
            duration = "უვადო (Google-ის წესების დაცვით)",
            accountType = "ახალი ვერიფიცირებული ანგარიში (New Account)",
            activationTerms = "მიღებისთანავე აუცილებელია შეცვალოთ პაროლი და მიაბათ თქვენი პირადი Recovery Email / 2-Step Verification. BEKASERVICE არასდროს ითხოვს თქვენს პირად 2FA კოდებს.",
            officialUrl = "https://mail.google.com/",
            termsUrl = "https://policies.google.com/terms",
            lastVerifiedAt = "2026-10-25",
            searchKeywords = "gmail phone verified account ჯიმეილი გუგლის ანგარიში ელფოსტა მეილი pva gmail",
            isFeatured = false,
            displayOrder = 13,
            brand = "Google",
            compatibleModels = "Android, iOS, Web, PC, Mac",
            warranty = "ჩაბარებისა და პირველადი შესვლის სრული გარანტია",
            brandColorHex = "#EA4335",
            brandBadgeText = "Gmail • Verified"
        ),
        ProductItem(
            id = 113,
            title = "Google Storage 2TB",
            price = "45 ₾",
            category = "პროგრამული სერვისები",
            type = "software",
            subcategory = SUBCAT_GOOGLE,
            imageUrl = "https://images.unsplash.com/photo-1544197150-b99a580bb7a8?w=800",
            shortDescription = "2 TB (2000 GB) ოფიციალური Google One ღრუბლოვანი მეხსიერება Google Photos, Google Drive და Gmail-ისთვის.",
            description = "Google One 2TB გეგმა ზრდის თქვენი Google ანგარიშის ღრუბლოვან საცავს 2000 გიგაბაიტამდე. იდეალურია ტელეფონის ფოტოებისა და 4K ვიდეოების ორიგინალ ხარისხში შესანახად (Google Photos), სამუშაო ფაილების სინქრონიზაციისთვის (Google Drive) და უწყვეტი ელფოსტისთვის (Gmail).",
            features = listOf(
                "2 TB (2000 GB) საერთო მეხსიერება Google Drive, Gmail და Google Photos-ში",
                "ფოტოებისა და 4K ვიდეოების ავტომატური სარეზერვო კოპირება სმარტფონიდან",
                "Google Photos-ის დამატებითი რედაქტირების ფუნქციები (Magic Eraser და სხვ.)",
                "ოჯახის ჯგუფში (5 წევრამდე) გაზიარების მხარდაჭერა — პირადი ფაილების იზოლაციით"
            ),
            targetAudience = "ფოტოგრაფები, iPhone და Android მომხმარებლები, რომლებსაც გადაევსოთ 15GB უფასო მეხსიერება, და ოჯახები.",
            offerType = "Google One Cloud Storage გამოწერა",
            duration = "12 თვე / 1 თვე (პაკეტის მიხედვით)",
            accountType = "პირად ანგარიშზე გააქტიურება (Invite / Upgrade) — პაროლის გაზიარების გარეშე",
            activationTerms = "აქტივაცია შესაძლებელია თქვენს არსებულ Gmail მისამართზე ოფიციალური Google One მოწვევით (პაროლის მოთხოვნის გარეშე!). თქვენი ფოტოები და ფაილები რჩება 100% კონფიდენციალური.",
            officialUrl = "https://one.google.com/about/plans",
            termsUrl = "https://one.google.com/terms-of-service",
            lastVerifiedAt = "2026-10-25",
            searchKeywords = "google storage 2tb გუგლ სტორეჯი 2 ტერაბაიტი google one მეხსიერება google drive photos",
            isFeatured = false,
            displayOrder = 14,
            brand = "Google",
            compatibleModels = "Android, iOS, Windows, macOS, Web",
            warranty = "აქტიური პერიოდის სრული გარანტია",
            brandColorHex = "#34A853",
            brandBadgeText = "Google One • 2TB"
        ),
        ProductItem(
            id = 114,
            title = "Google Storage 5TB",
            price = "90 ₾",
            category = "პროგრამული სერვისები",
            type = "software",
            subcategory = SUBCAT_GOOGLE,
            imageUrl = "https://images.unsplash.com/photo-1558494949-ef010cbdcc31?w=800",
            shortDescription = "5 TB (5000 GB) გაფართოებული Google One ღრუბლოვანი საცავი დიდი მოცულობის ვიდეო არქივების, RAW ფოტოებისა და ბიზნეს ფაილებისთვის.",
            description = "Google Storage 5TB განკუთვნილია მომხმარებლებისთვის, რომლებსაც სჭირდებათ დიდი მოცულობის ღრუბლოვანი საცავი Google Drive-ში, Google Photos-სა და Gmail-ში. უზრუნველყოფს სწრაფ სინქრონიზაციას ყველა მოწყობილობას შორის და ფაილების დაცულ შენახვას.",
            features = listOf(
                "5 TB (5000 GB) ღრუბლოვანი მეხსიერება Google Drive, Photos და Gmail-ისთვის",
                "იდეალურია 4K/8K ვიდეო მასალებისა და დიდი პროექტების სარეზერვო ასლებისთვის",
                "100% კონფიდენციალური პირადი სივრცე თქვენს ანგარიშზე",
                "მხარდაჭერა Google Drive Desktop სინქრონიზაციის აპლიკაციაში"
            ),
            targetAudience = "ვიდეოგრაფები, ფოტოგრაფები, სტუდიები და მომხმარებლები დიდი ციფრული არქივით.",
            offerType = "Google One 5TB გამოწერა",
            duration = "12 თვე / 1 თვე (ინფორმაცია ზუსტდება შეკვეთისას)",
            accountType = "პირად ანგარიშზე გააქტიურება (Upgrade / Family Plan)",
            activationTerms = "აქტიურდება პირდაპირ თქვენს Google ანგარიშზე პაროლის გადაცემის გარეშე. ფაილებზე წვდომა გაქვთ მხოლოდ თქვენ.",
            officialUrl = "https://one.google.com/about/plans",
            termsUrl = "https://one.google.com/terms-of-service",
            lastVerifiedAt = "2026-10-25",
            searchKeywords = "google storage 5tb გუგლ სტორეჯი 5 ტერაბაიტი google one 5tb მეხსიერება",
            isFeatured = false,
            displayOrder = 15,
            brand = "Google",
            compatibleModels = "Android, iOS, Windows, macOS, Web",
            warranty = "აქტიური პერიოდის სრული გარანტია",
            brandColorHex = "#FBBC04",
            brandBadgeText = "Google One • 5TB"
        ),
        ProductItem(
            id = 115,
            title = "Google AI Pro + Storage Bundle",
            price = "95 ₾",
            category = "პროგრამული სერვისები",
            type = "software",
            subcategory = SUBCAT_GOOGLE,
            imageUrl = "https://images.unsplash.com/photo-1573164713988-8665fc963095?w=800",
            shortDescription = "ოფიციალური კომბინირებული პაკეტი (Google One AI Premium): Gemini 2.5 Pro AI ასისტენტი + 2TB Google Drive/Photos მეხსიერება ერთ გამოწერაში.",
            description = "Google-ის ოფიციალური Google One AI Premium გეგმა ბუნებრივად აერთიანებს ორ მთავარ პროდუქტს ერთ ბანდლში: Google AI Pro (Gemini Advanced / 2.5 Pro, NotebookLM Plus, Workspace AI) და 2TB (2000 GB) Google One ღრუბლოვან მეხსიერებას. ცალ-ცალკე შეძენის ნაცვლად იღებთ სრულ AI და Cloud ეკოსისტემას ერთიან ფასად.",
            features = listOf(
                "ოფიციალური კომბინირებული პაკეტი: Gemini Advanced AI + 2TB Google Cloud",
                "Gemini პირდაპირ Docs, Gmail, Sheets და Drive აპლიკაციებში",
                "2000 GB საცავი ფოტოების, ვიდეოებისა და დოკუმენტებისთვის",
                "NotebookLM Plus და Deep Research კვლევითი ფუნქციონალი"
            ),
            targetAudience = "მომხმარებლები, რომლებსაც სჭირდებათ როგორც Google-ის 2TB მეხსიერება, ისე Gemini Pro AI ასისტენტი.",
            offerType = "კომბინირებული ბანდლი (AI + 2TB Cloud Bundle)",
            duration = "18 თვე (სპეც-შეთავაზება) ან სტანდარტული პერიოდი",
            accountType = "პირადი ანგარიში (Private Account)",
            activationTerms = "ეფუძნება Google One AI Premium-ის ოფიციალურ ბანდლს. 18-თვიანი პერიოდი ხელმისაწვდომია BEKASERVICE-ის სპეციალური აქციის ფარგლებში.",
            officialUrl = "https://one.google.com/about/ai-premium/",
            termsUrl = "https://one.google.com/terms-of-service",
            lastVerifiedAt = "2026-10-25",
            searchKeywords = "google ai pro storage bundle გუგლ აი პრო მეხსიერება ბანდლი 2tb gemini bundle",
            isFeatured = false,
            displayOrder = 16,
            brand = "Google",
            compatibleModels = "Android, iOS, Web, Windows, macOS",
            warranty = "აქტივაციის სრული გარანტია",
            brandColorHex = "#4285F4",
            brandBadgeText = "AI Pro + 2TB"
        ),

        // =====================================================================
        // C. ვიდეო, დიზაინი და კონტენტის შექმნა (5 პროდუქტი: ID 116 - 120)
        // =====================================================================
        ProductItem(
            id = 116,
            title = "CapCut Pro",
            price = "35 ₾",
            category = "პროგრამული სერვისები",
            type = "software",
            subcategory = SUBCAT_CREATIVE,
            imageUrl = "https://images.unsplash.com/photo-1574717024653-61fd2cf4d44d?w=800",
            shortDescription = "პროფესიონალური ვიდეო მონტაჟი: AI ავტო-სუბტიტრები, ფონის მოცილება, 4K 60fps ექსპორტი წყლის ნიშნის (Watermark) გარეშე და პრემიუმ ეფექტები.",
            description = "CapCut Pro არის თანამედროვე ვიდეო მონტაჟის წამყვანი პლატფორმა სმარტფონებისა და კომპიუტერებისთვის. Pro გამოწერა ხსნის ყველა ფასიან ვიზუალურ ეფექტს, გადასვლას (Transitions), AI Auto Captions-ს, ხმის იზოლაციას, ფონის ერთი ღილაკით მოშორებას, ფერების კორექციასა და CapCut Cloud საცავს.",
            features = listOf(
                "ყველა Pro ეფექტის, ფილტრის, ანიმაციისა და შრიფტის სრული განბლოკვა",
                "AI Auto Captions, Vocal Isolation (ხმის გასუფთავება) და AI Background Removal",
                "4K 60fps უმაღლესი ხარისხის ექსპორტი Watermark-ის გარეშე",
                "მუშაობს Android, iOS, Windows და macOS ვერსიებზე"
            ),
            targetAudience = "TikTok, Instagram Reels, YouTube Shorts შემქმნელები, მონტაჟისტები და სოციალური მედიის მენეჯერები.",
            offerType = "თვიური გამოწერა (CapCut Pro)",
            duration = "1 თვე",
            accountType = "პირადი ან გაზიარებული პრო-ანგარიში (პაკეტის მიხედვით)",
            activationTerms = "აქტივაციის ტიპი (პირადი ან მზა ანგარიში) თანხმდება შეკვეთისას. მყისიერი ჩართვა ტელეფონსა და კომპიუტერზე.",
            officialUrl = "https://www.capcut.com/",
            termsUrl = "https://www.capcut.com/clause/terms-of-service",
            lastVerifiedAt = "2026-10-25",
            searchKeywords = "capcut pro კაპკატი კაპკატ პრო ვიდეო მონტაჟი რედაქტირება reels tiktok",
            isFeatured = false,
            displayOrder = 17,
            brand = "CapCut",
            compatibleModels = "Android, iOS, Windows, macOS, Web",
            warranty = "1 თვე სრული გარანტია",
            brandColorHex = "#00F0FF",
            brandBadgeText = "CapCut • Pro"
        ),
        ProductItem(
            id = 117,
            title = "Adobe Creative Cloud",
            price = "90 ₾",
            category = "პროგრამული სერვისები",
            type = "software",
            subcategory = SUBCAT_CREATIVE,
            imageUrl = "https://images.unsplash.com/photo-1626785774573-4b799315345d?w=800",
            shortDescription = "Adobe-ის 20+ ოფიციალური პროგრამის სრული პაკეტი: Photoshop, Illustrator, Premiere Pro, After Effects, Lightroom + Adobe Firefly AI.",
            description = "Adobe Creative Cloud (All Apps) აერთიანებს მსოფლიოს ინდუსტრიულ სტანდარტად აღიარებულ გრაფიკულ, ვიდეო და დიზაინერულ პროგრამებს. მოიცავს Photoshop, Illustrator, Premiere Pro, After Effects, InDesign, Acrobat Pro, Lightroom, Adobe Fonts და Adobe Firefly გენერაციულ AI კრედიტებს.",
            features = listOf(
                "20+ ოფიციალური Desktop და Mobile აპლიკაცია (Photoshop, Illustrator, Premiere Pro, AE)",
                "Adobe Firefly გენერაციული AI (Generative Fill & Expand)",
                "100 GB-მდე Creative Cloud მეხსიერება, Adobe Fonts და Portfolio",
                "ოფიციალური განახლებები Creative Cloud Desktop აპლიკაციიდან (Windows / macOS)"
            ),
            targetAudience = "გრაფიკული დიზაინერები, ვიდეო მონტაჟისტები, ფოტოგრაფები, არქიტექტორები და კრეატიული სააგენტოები.",
            offerType = "სააბონენტო გეგმა (Creative Cloud All Apps)",
            duration = "1 თვე (ხელმისაწვდომია 3/6/12-თვიანი პაკეტებიც)",
            accountType = "პირადი ანგარიში (Private Account / ოფიციალური ლიცენზია)",
            activationTerms = "პროგრამები იტვირთება პირდაპირ Adobe-ის ოფიციალური სერვერიდან (Creative Cloud App). არანაირი არაოფიციალური კრეკი ან საფრთხის შემცველი ფაილი.",
            officialUrl = "https://www.adobe.com/creativecloud.html",
            termsUrl = "https://www.adobe.com/legal/terms.html",
            lastVerifiedAt = "2026-10-25",
            searchKeywords = "adobe creative cloud ადობე ფოტოშოპი ილუსტრატორი პრემიერ პრო photoshop illustrator premiere after effects",
            isFeatured = false,
            displayOrder = 18,
            brand = "Adobe",
            compatibleModels = "Windows, macOS, iPadOS, iOS, Android",
            warranty = "აქტიური გამოწერის სრული პერიოდი",
            brandColorHex = "#FF0000",
            brandBadgeText = "Adobe • All Apps"
        ),
        ProductItem(
            id = 118,
            title = "Canva Pro",
            price = "25 ₾",
            category = "პროგრამული სერვისები",
            type = "software",
            subcategory = SUBCAT_CREATIVE,
            imageUrl = "https://images.unsplash.com/photo-1611162617474-5b21e879e113?w=800",
            shortDescription = "Canva-ს პრემიუმ გრაფიკული დიზაინის პლატფორმა: 100M+ პრემიუმ ფოტო/შაბლონი, Magic Studio AI, Background Remover და Brand Kit.",
            description = "Canva Pro არის ყველაზე მოსახერხებელი პლატფორმა სოციალური მედიის პოსტების, პრეზენტაციების, სარეკლამო ბანერების, ლოგოებისა და ვიდეოების შესაქმნელად. ხსნის წვდომას 100 მილიონზე მეტ პრემიუმ სურათზე, ვიდეოსა და გრაფიკულ ელემენტზე, ერთი ღილაკით ფონის მოშორებაზე (Background Remover), Magic Switch ზომის შეცვლასა და Magic Studio AI ხელსაწყოებზე.",
            features = listOf(
                "100+ მილიონი პრემიუმ შაბლონი, ფოტო, ვიდეო, აუდიო და გრაფიკული ელემენტი",
                "Background Remover — ფოტოსა და ვიდეოს ფონის 1 კლიკით მოშორება",
                "Magic Studio AI (Magic Write, Magic Eraser, Magic Expand, Magic Resize)",
                "Brand Kit — საკუთარი შრიფტების, ფერებისა და ლოგოების მართვა",
                "მაღალი გარჩევადობის ექსპორტი (PNG გამჭვირვალე ფონით, SVG, PDF Print)"
            ),
            targetAudience = "სოციალური მედიის მენეჯერები, მცირე და საშუალო ბიზნესი, სტუდენტები, მასწავლებლები და მარკეტერები.",
            offerType = "სააბონენტო წვდომა (Canva Pro / Team Invite)",
            duration = "1 თვე / 12 თვე (არჩევანით)",
            accountType = "პირად ანგარიშზე გააქტიურება (Invite თქვენს ელფოსტაზე — პაროლის გარეშე)",
            activationTerms = "აქტიურდება პირდაპირ თქვენს არსებულ Canva ანგარიშზე ელფოსტის მოწვევით (პაროლის გაზიარება საჭირო არ არის). თქვენი დიზაინები ჩანს მხოლოდ თქვენთან.",
            officialUrl = "https://www.canva.com/pro/",
            termsUrl = "https://www.canva.com/policies/terms-of-use/",
            lastVerifiedAt = "2026-10-25",
            searchKeywords = "canva pro კანვა პრო დიზაინი ბანერები პრეზენტაცია გრაფიკა",
            isFeatured = true,
            displayOrder = 3,
            brand = "Canva",
            compatibleModels = "Web, Android, iOS, Windows, macOS",
            warranty = "აქტიური პერიოდის სრული გარანტია",
            brandColorHex = "#7C3AED",
            brandBadgeText = "Canva • Pro"
        ),
        ProductItem(
            id = 119,
            title = "YouTube Premium",
            price = "20 ₾",
            category = "პროგრამული სერვისები",
            type = "software",
            subcategory = SUBCAT_CREATIVE,
            imageUrl = "https://images.unsplash.com/photo-1611162616475-46b635cb6868?w=800",
            shortDescription = "YouTube და YouTube Music რეკლამების გარეშე, ფონური მოსმენა ჩამქრალ ეკრანზე და ვიდეოების ოფლაინ ჩამოტვირთვა.",
            description = "YouTube Premium გაძლევთ საშუალებას უყუროთ ნებისმიერ ვიდეოს სრულიად უ-რეკლამოდ ტელეფონში, Smart TV-სა და კომპიუტერში. მოიცავს ფონურ რეჟიმს (Background Play), ოფლაინ ჩამოტვირთვას, 1080p Premium გაუმჯობესებულ ბიტრეიტს და სრულ წვდომას YouTube Music Premium აპლიკაციაზე.",
            features = listOf(
                "YouTube ვიდეოების ყურება ნულოვანი რეკლამით ყველა მოწყობილობაზე",
                "Background Play — მოსმენა ეკრანის დაბლოკვის ან სხვა აპში გადასვლის დროს",
                "YouTube Music Premium — მილიონობით სიმღერა უ-რეკლამოდ და ოფლაინ რეჟიმში",
                "ვიდეოების ჩამოტვირთვა ინტერნეტის გარეშე საყურებლად"
            ),
            targetAudience = "ყველა მომხმარებელი, ვინც ყოველდღიურად უყურებს YouTube-ს ტელეფონში, ტელევიზორში ან უსმენს მუსიკას მგზავრობისას.",
            offerType = "თვიური ან წლიური გამოწერა",
            duration = "1 თვე (ხელმისაწვდომია 6/12-თვიანი პაკეტიც)",
            accountType = "პირად ანგარიშზე გააქტიურება (Invite / Individual)",
            activationTerms = "აქტიურდება თქვენს Google/YouTube ანგარიშზე ოჯახური მოწვევით ან ინდივიდუალური გამოწერით, თქვენი არხისა და ისტორიის სრული შენარჩუნებით.",
            officialUrl = "https://www.youtube.com/premium",
            termsUrl = "https://www.youtube.com/t/terms_paidservice",
            lastVerifiedAt = "2026-10-25",
            searchKeywords = "youtube premium იუთუბ პრემიუმი იუთუბი უ-რეკლამო youtube music",
            isFeatured = false,
            displayOrder = 19,
            brand = "YouTube",
            compatibleModels = "Android, iOS, Smart TV, Web, PlayStation, Xbox",
            warranty = "აქტიური პერიოდის სრული გარანტია",
            brandColorHex = "#FF0033",
            brandBadgeText = "YouTube • Premium"
        ),
        ProductItem(
            id = 120,
            title = "Spotify Premium",
            price = "18 ₾",
            category = "პროგრამული სერვისები",
            type = "software",
            subcategory = SUBCAT_CREATIVE,
            imageUrl = "https://images.unsplash.com/photo-1614680376593-902f749f7ffc?w=800",
            shortDescription = "მსოფლიოს #1 მუსიკალური სტრიმინგი: უ-რეკლამო მოსმენა, სიმღერების ოფლაინ ჩამოტვირთვა, შეუზღუდავი გადართვა და Very High აუდიო ხარისხი.",
            description = "Spotify Premium გაძლევთ სრულ თავისუფლებას მუსიკისა და პოდკასტების მოსმენისას: რეკლამების გარეშე, ნებისმიერი ტრეკის არჩევით, ოფლაინ ჩამოტვირთვით, AI DJ ფუნქციითა და უმაღლესი (320 kbps) აუდიო ხარისხით.",
            features = listOf(
                "მუსიკის მოსმენა სარეკლამო წყვეტების გარეშე",
                "ალბომებისა და ფლეილისტების ჩამოტვირთვა ოფლაინ მოსასმენად",
                "Very High აუდიო ხარისხი და შეუზღუდავი ტრეკების გადართვა (Skips)",
                "Spotify Jam და AI Playlist ფუნქციონალი"
            ),
            targetAudience = "მუსიკისა და პოდკასტების მოყვარულები ნებისმიერ სმარტფონსა და ავტომობილში (CarPlay / Android Auto).",
            offerType = "თვიური / მრავალთვიანი გამოწერა",
            duration = "1 თვე / 3 თვე / 12 თვე (არჩევანით)",
            accountType = "პირადი ანგარიში (Individual Upgrade / Family Invite)",
            activationTerms = "აქტიურდება თქვენს არსებულ ან ახალ Spotify ანგარიშზე ფლეილისტების სრული შენარჩუნებით.",
            officialUrl = "https://www.spotify.com/premium/",
            termsUrl = "https://www.spotify.com/legal/end-user-agreement/",
            lastVerifiedAt = "2026-10-25",
            searchKeywords = "spotify premium სპოტიფაი პრემიუმი მუსიკა სიმღერები",
            isFeatured = false,
            displayOrder = 20,
            brand = "Spotify",
            compatibleModels = "Android, iOS, Windows, macOS, CarPlay, Android Auto",
            warranty = "აქტიური პერიოდის სრული გარანტია",
            brandColorHex = "#1DB954",
            brandBadgeText = "Spotify • Premium"
        ),

        // =====================================================================
        // D. პროგრამული უზრუნველყოფის ლიცენზიები (4 პროდუქტი: ID 121 - 124)
        // =====================================================================
        ProductItem(
            id = 121,
            title = "Windows 11 Pro Retail Key",
            price = "45 ₾",
            category = "პროგრამული სერვისები",
            type = "software",
            subcategory = SUBCAT_LICENSES,
            imageUrl = "https://images.unsplash.com/photo-1633419461186-7d40a38105ec?w=800",
            shortDescription = "ოფიციალური Windows 11 Pro 25-ნიშნა ციფრული Retail გასაღები — უვადო აქტივაცია, BitLocker, Remote Desktop და განახლებების სრული მხარდაჭერა.",
            description = "Windows 11 Pro Retail Key არის ოფიციალური 25-სიმბოლოიანი ციფრული ლიცენზიის გასაღები (XXXXX-XXXXX-XXXXX-XXXXX-XXXXX). OEM გასაღებისგან განსხვავებით, Retail ლიცენზია არ იბლოკება დედაპლატის შეცვლისას და შესაძლებელია მისი გადატანა ახალ კომპიუტერზე (1 აქტიურ მოწყობილობაზე ერთდროულად). ხსნის Pro ვერსიის ყველა ფუნქციას: BitLocker დაშიფვრას, Remote Desktop-ს, Hyper-V ვირტუალიზაციას და ოფიციალურ Windows Update-ებს.",
            features = listOf(
                "ოფიციალური 25-ნიშნა Retail აქტივაციის გასაღები 1 კომპიუტერისთვის",
                "Retail უპირატესობა: გადატანადია სხვა კომპიუტერზე და იბმება Microsoft ანგარიშზე",
                "სრული Pro ფუნქციონალი: BitLocker, Remote Desktop, Hyper-V, Group Policy",
                "მუშაობს ახალ ინსტალაციაზე და Windows 10 Pro -> 11 Pro განახლებაზე"
            ),
            targetAudience = "პერსონალური კომპიუტერებისა და ლეპტოპების მფლობელები, გეიმერები, IT სპეციალისტები და ოფისები.",
            offerType = "უვადო ციფრული ლიცენზია (Perpetual Retail Key)",
            duration = "უვადო (Lifetime Retail License)",
            accountType = "ლიცენზიის გასაღები (25-Character Retail Product Key)",
            activationTerms = "აქტივაცია ხდება სტანდარტულად: Settings -> System -> Activation -> Change Product Key. საჭიროა კომპიუტერზე ეყენოს ოფიციალური Windows 11 Pro რელიზი.",
            officialUrl = "https://www.microsoft.com/en-us/d/windows-11-pro/dg7gmgf0d8h4",
            termsUrl = "https://www.microsoft.com/en-us/Useterms/Retail/Windows/11/Useterms_Retail_Windows_11_English.htm",
            lastVerifiedAt = "2026-10-25",
            searchKeywords = "windows 11 pro retail key ვინდოუს 11 პრო გასაღები ლიცენზია აქტივაცია microsoft windows",
            isFeatured = false,
            displayOrder = 21,
            brand = "Microsoft",
            compatibleModels = "PC / Laptop (Windows 11 Pro 64-bit)",
            warranty = "ონლაინ აქტივაციის სრული გარანტია",
            brandColorHex = "#00A4EF",
            brandBadgeText = "Windows 11 • Retail"
        ),
        ProductItem(
            id = 122,
            title = "Office LTSC 2024",
            price = "65 ₾",
            category = "პროგრამული სერვისები",
            type = "software",
            subcategory = SUBCAT_LICENSES,
            imageUrl = "https://images.unsplash.com/photo-1497215728101-856f4ea42174?w=800",
            shortDescription = "Microsoft Office LTSC Professional Plus 2024 — უვადო კლასიკური საოფისე პაკეტი (Word, Excel, PowerPoint, Outlook, Access) ყოველთვიური გადასახადის გარეშე.",
            description = "Office LTSC 2024 (Long-Term Servicing Channel) არის Microsoft-ის უახლესი თაობის კლასიკური საოფისე პაკეტი მათთვის, ვისაც სურს Word, Excel, PowerPoint, Outlook და OneNote ერთჯერადი ლიცენზიით, ყოველთვიური გამოწერის გარეშე. მოიცავს Excel-ის განახლებულ დინამიკურ დიაგრამებს, გაუმჯობესებულ წარმადობასა და სრულ ოფლაინ მუშაობას.",
            features = listOf(
                "სრული საოფისე კომპლექტი: Word, Excel, PowerPoint, Outlook, OneNote, Access (PC)",
                "ერთჯერადი აქტივაცია — არ საჭიროებს ყოველთვიურ ან წლიურ გადასახადს",
                "მუშაობს სრულად ოფლაინ რეჟიმში ინტერნეტზე დამოკიდებულების გარეშე",
                "2024 წლის განახლებული ინტერფეისი და გაზრდილი სისწრაფე"
            ),
            targetAudience = "ბუღალტრები, სტუდენტები, საჯარო და კერძო ორგანიზაციების თანამშრომლები, რომლებსაც სურთ მუდმივი საოფისე პაკეტი.",
            offerType = "ერთჯერადი ლიცენზიის გასაღები (LTSC 2024 License)",
            duration = "უვადო (1 კომპიუტერზე)",
            accountType = "ლიცენზიის გასაღები + ოფიციალური საინსტალაციო ინსტრუქცია",
            activationTerms = "თავსებადია Windows 10 და Windows 11 სისტემებთან. ინსტალაციის წინ რეკომენდებულია ძველი არააქტივირებული Office ვერსიის წაშლა.",
            officialUrl = "https://learn.microsoft.com/en-us/office/ltsc/2024/overview",
            termsUrl = "https://www.microsoft.com/licensing/terms",
            lastVerifiedAt = "2026-10-25",
            searchKeywords = "office ltsc 2024 ოფისი 2024 ვორდი ექსელი პოვერპოინტი word excel powerpoint microsoft office",
            isFeatured = false,
            displayOrder = 22,
            brand = "Microsoft",
            compatibleModels = "Windows 10 / Windows 11 (ასევე macOS ვერსია შეთანხმებით)",
            warranty = "აქტივაციის სრული გარანტია",
            brandColorHex = "#D83B01",
            brandBadgeText = "Office • LTSC 2024"
        ),
        ProductItem(
            id = 123,
            title = "Microsoft 365 Personal",
            price = "85 ₾",
            category = "პროგრამული სერვისები",
            type = "software",
            subcategory = SUBCAT_LICENSES,
            imageUrl = "https://images.unsplash.com/photo-1517694712202-14dd9538aa97?w=800",
            shortDescription = "ოფიციალური Microsoft 365 გამოწერა 1 მომხმარებლისთვის 5 მოწყობილობაზე (PC, Mac, iPad, iPhone, Android) + 1TB OneDrive ღრუბლოვანი საცავი.",
            description = "Microsoft 365 Personal არის ოფიციალური ღრუბლოვანი საოფისე ეკოსისტემა, რომელიც მოიცავს მუდმივად განახლებად Word, Excel, PowerPoint, Outlook, OneNote და Defender აპლიკაციებს. ერთი ანგარიშით შეგიძლიათ ერთდროულად გამოიყენოთ 5 მოწყობილობაზე (Windows PC, MacBook, iPad, iPhone, Android) და ისარგებლოთ 1TB (1000 GB) OneDrive დაცული მეხსიერებით.",
            features = listOf(
                "Word, Excel, PowerPoint, Outlook და OneNote უახლესი ოფიციალური ვერსიები",
                "1 TB (1000 GB) OneDrive ღრუბლოვანი მეხსიერება Personal Vault დაცვით",
                "მუშაობს ერთდროულად 5 მოწყობილობაზე: Windows, macOS, iPadOS, iOS და Android",
                "Microsoft Copilot AI ფუნქციების მხარდაჭერა საოფისე პროგრამებში"
            ),
            targetAudience = "მომხმარებლები, რომლებიც მუშაობენ რამდენიმე მოწყობილობაზე (მაგ. MacBook + iPhone ან PC + პლანშეტი) და სჭირდებათ OneDrive სინქრონიზაცია.",
            offerType = "12-თვიანი ან თვიური გამოწერა (Microsoft 365)",
            duration = "12 თვე (წლიური პაკეტი)",
            accountType = "პირად Microsoft ანგარიშზე გააქტიურება ან ლიცენზია",
            activationTerms = "აქტიურდება პირდაპირ მომხმარებლის Microsoft ანგარიშზე (account.microsoft.com).",
            officialUrl = "https://www.microsoft.com/en-us/microsoft-365/p/microsoft-365-personal/cfq7ttc0k5bf",
            termsUrl = "https://www.microsoft.com/en-us/servicesagreement",
            lastVerifiedAt = "2026-10-25",
            searchKeywords = "microsoft 365 personal office 365 მაიკროსოფტ 365 ოფის 365 onedrive 1tb mac office",
            isFeatured = false,
            displayOrder = 23,
            brand = "Microsoft",
            compatibleModels = "Windows, macOS, iPad, iPhone, Android (5 მოწყობილობა)",
            warranty = "12 თვე ოფიციალური გარანტია",
            brandColorHex = "#0078D4",
            brandBadgeText = "Microsoft 365 • 1TB"
        ),
        ProductItem(
            id = 124,
            title = "Office 2019",
            price = "40 ₾",
            category = "პროგრამული სერვისები",
            type = "software",
            subcategory = SUBCAT_LICENSES,
            imageUrl = "https://images.unsplash.com/photo-1454165804606-c3d57bc86b40?w=800",
            shortDescription = "Microsoft Office Professional Plus 2019 — კლასიკური, სწრაფი და ნაცადი საოფისე პაკეტის უვადო ლიცენზიის გასაღები Windows კომპიუტერებისთვის.",
            description = "Office 2019 Professional Plus წარმოადგენს დროში გამოცდილ, მსუბუქ და სტაბილურ საოფისე პაკეტს Windows 10 და Windows 11 კომპიუტერებისთვის. მოიცავს Word 2019, Excel 2019, PowerPoint 2019, Outlook 2019, Publisher და Access პროგრამებს ერთჯერადი უვადო აქტივაციით.",
            features = listOf(
                "სრული კლასიკური პაკეტი: Word, Excel, PowerPoint, Outlook, Access, Publisher",
                "დაბალი სისტემური მოთხოვნები — მუშაობს სწრაფად შედარებით ძველ ლეპტოპებზეც",
                "ერთჯერადი 25-ნიშნა აქტივაციის გასაღები ყოველთვიური გადასახადის გარეშე"
            ),
            targetAudience = "სტუდენტები, საოფისე მომხმარებლები და სტანდარტული კონფიგურაციის კომპიუტერების მფლობელები.",
            offerType = "ერთჯერადი ციფრული გასაღები (Office 2019 Key)",
            duration = "უვადო (1 კომპიუტერზე)",
            accountType = "ლიცენზიის გასაღები (Product Key)",
            activationTerms = "განკუთვნილია Windows 10 და Windows 11-ისთვის. მოყვება ოფიციალური საინსტალაციო ბმული და აქტივაციის ინსტრუქცია.",
            officialUrl = "https://support.microsoft.com/en-us/office",
            termsUrl = "https://www.microsoft.com/licensing/terms",
            lastVerifiedAt = "2026-10-25",
            searchKeywords = "office 2019 ოფისი 2019 word excel powerpoint 2019 გასაღები ლიცენზია",
            isFeatured = false,
            displayOrder = 24,
            brand = "Microsoft",
            compatibleModels = "Windows 10 / Windows 11 PC",
            warranty = "აქტივაციის სრული გარანტია",
            brandColorHex = "#EB3C00",
            brandBadgeText = "Office • 2019"
        )
        )
    }
}

/**
 * ავთენტური ბრენდ-ვიზუალი ყოველი პროგრამული სერვისისთვის.
 * უზრუნველყოფს, რომ არცერთი სერვისის ქარდი არ ჩანდეს ცარიელი ან ერთნაირი placeholder-ით,
 * თუნდაც ნელი ინტერნეტის დროს, და ზუსტად გადმოსცემდეს ბრენდის ვიზუალურ იდენტობას.
 */
@Composable
fun SoftwareBrandVisualHeader(
    product: ProductItem,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    ignoreCustomImage: Boolean = false
) {
    if (!ignoreCustomImage && ProductImageManager.hasCustomAttachedPhoto(product)) {
        ProductSmartVisual(
            product = product,
            modifier = modifier,
            compact = compact,
            forceAutoGenerated = false
        )
        return
    }

    val titleLower = product.title.lowercase()
    val brandLower = product.brand.lowercase()
    val catLower = product.category.lowercase()
    val brandStyle = when {
        titleLower.contains("chatgpt") || brandLower.contains("openai") -> Triple(
            listOf(Color(0xFF0A2E24), Color(0xFF0F172A)),
            Color(0xFF10A37F),
            "OpenAI"
        )
        titleLower.contains("google ai") || titleLower.contains("gemini") -> Triple(
            listOf(Color(0xFF172554), Color(0xFF3B0764), Color(0xFF0F172A)),
            Color(0xFF60A5FA),
            "Google Gemini"
        )
        titleLower.contains("gmail") -> Triple(
            listOf(Color(0xFF3B1219), Color(0xFF111827)),
            Color(0xFFEA4335),
            "Gmail PVA"
        )
        titleLower.contains("google storage") || titleLower.contains("google one") -> Triple(
            listOf(Color(0xFF0D2E22), Color(0xFF172554)),
            Color(0xFF34A853),
            "Google One"
        )
        titleLower.contains("grok") || brandLower.contains("xai") -> Triple(
            listOf(Color(0xFF090D16), Color(0xFF1E293B)),
            Color(0xFFE2E8F0),
            "xAI Grok"
        )
        titleLower.contains("claude") || brandLower.contains("anthropic") -> Triple(
            listOf(Color(0xFF2E1B14), Color(0xFF141210)),
            Color(0xFFD97757),
            "Anthropic"
        )
        titleLower.contains("perplexity") -> Triple(
            listOf(Color(0xFF0B2930), Color(0xFF0F172A)),
            Color(0xFF20B8CD),
            "Perplexity"
        )
        titleLower.contains("cursor") -> Triple(
            listOf(Color(0xFF0F172A), Color(0xFF1E1B4B)),
            Color(0xFF38BDF8),
            "Cursor IDE"
        )
        titleLower.contains("lovable") -> Triple(
            listOf(Color(0xFF3B0A2A), Color(0xFF1E1B4B)),
            Color(0xFFEC4899),
            "Lovable.dev"
        )
        titleLower.contains("capcut") -> Triple(
            listOf(Color(0xFF082830), Color(0xFF111827)),
            Color(0xFF00F0FF),
            "CapCut Pro"
        )
        titleLower.contains("pdf") || titleLower.contains("acrobat") -> Triple(
            listOf(Color(0xFF3B0A0F), Color(0xFF161218)),
            Color(0xFFFF4D4F),
            product.brand.ifBlank { "PDF Pro" }
        )
        titleLower.contains("adobe") -> Triple(
            listOf(Color(0xFF3B0A0A), Color(0xFF141418)),
            Color(0xFFFF3B30),
            "Adobe CC"
        )
        titleLower.contains("canva") -> Triple(
            listOf(Color(0xFF004C56), Color(0xFF3B0764)),
            Color(0xFF00C4CC),
            "Canva Pro"
        )
        titleLower.contains("youtube") -> Triple(
            listOf(Color(0xFF3B0A12), Color(0xFF121216)),
            Color(0xFFFF0033),
            "YouTube"
        )
        titleLower.contains("spotify") -> Triple(
            listOf(Color(0xFF092E1A), Color(0xFF0F1412)),
            Color(0xFF1DB954),
            "Spotify"
        )
        titleLower.contains("netflix") -> Triple(
            listOf(Color(0xFF33060A), Color(0xFF0F0F12)),
            Color(0xFFE50914),
            "Netflix"
        )
        titleLower.contains("telegram") -> Triple(
            listOf(Color(0xFF09253A), Color(0xFF0F172A)),
            Color(0xFF2AABEE),
            "Telegram"
        )
        titleLower.contains("discord") -> Triple(
            listOf(Color(0xFF181B3E), Color(0xFF0F1222)),
            Color(0xFF5865F2),
            "Discord"
        )
        titleLower.contains("midjourney") -> Triple(
            listOf(Color(0xFF111827), Color(0xFF311042)),
            Color(0xFFA855F7),
            "Midjourney"
        )
        titleLower.contains("windows") -> Triple(
            listOf(Color(0xFF082644), Color(0xFF0F172A)),
            Color(0xFF00A4EF),
            "Windows"
        )
        titleLower.contains("microsoft 365") || titleLower.contains("office") -> Triple(
            listOf(Color(0xFF361509), Color(0xFF172554)),
            Color(0xFFFF5722),
            "Microsoft"
        )
        catLower.contains("ეკრან") -> Triple(
            listOf(Color(0xFF0D2137), Color(0xFF12151E)),
            Color(0xFF38BDF8),
            product.brand.ifBlank { "OLED Display" }
        )
        catLower.contains("ელემენტ") -> Triple(
            listOf(Color(0xFF0B2B1D), Color(0xFF12151E)),
            Color(0xFF4ADE80),
            product.brand.ifBlank { "Battery 100%" }
        )
        catLower.contains("კამერ") -> Triple(
            listOf(Color(0xFF251438), Color(0xFF12151E)),
            Color(0xFFC084FC),
            product.brand.ifBlank { "Pro Camera" }
        )
        else -> Triple(
            listOf(Color(0xFF1F1610), Color(0xFF12151E)),
            Color(0xFFFF7A00),
            product.brand.ifBlank { product.category.ifBlank { "BEKASERVICE" } }
        )
    }

    val (gradientColors, accentColor, brandTitle) = brandStyle

    Box(
        modifier = modifier
            .background(Brush.linearGradient(gradientColors))
            .padding(if (compact) 10.dp else 16.dp)
    ) {
        // დეკორატიული ნათება
        Box(
            modifier = Modifier
                .size(if (compact) 68.dp else 110.dp)
                .align(Alignment.Center)
                .clip(CircleShape)
                .background(accentColor.copy(alpha = 0.16f))
        )

        // ცენტრალური ოფიციალური ბრენდ-ემბლემა
        Surface(
            shape = RoundedCornerShape(if (compact) 14.dp else 20.dp),
            color = Color(0xFF0D1017).copy(alpha = 0.88f),
            border = BorderStroke(1.5.dp, accentColor.copy(alpha = 0.75f)),
            modifier = Modifier
                .align(Alignment.Center)
                .defaultMinSize(
                    minWidth = if (compact) 54.dp else 76.dp,
                    minHeight = if (compact) 54.dp else 76.dp
                )
        ) {
            Column(
                modifier = Modifier.padding(horizontal = if (compact) 12.dp else 18.dp, vertical = if (compact) 8.dp else 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                BrandMarkIcon(
                    titleLower = titleLower,
                    categoryLower = catLower,
                    accentColor = accentColor,
                    compact = compact
                )
                Spacer(modifier = Modifier.height(if (compact) 3.dp else 6.dp))
                Text(
                    text = brandTitle,
                    color = Color.White,
                    fontSize = if (compact) 10.sp else 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // თუ სპეციალური ხანგრძლივობა ან ტიპია მითითებული, ვაჩვენებთ ზედა მარცხენა კუთხეში
        if (product.duration.isNotBlank()) {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color.Black.copy(alpha = 0.72f),
                border = BorderStroke(0.8.dp, accentColor.copy(alpha = 0.6f)),
                modifier = Modifier.align(Alignment.TopStart)
            ) {
                Text(
                    text = product.duration.take(22),
                    color = accentColor,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun BrandMarkIcon(
    titleLower: String,
    categoryLower: String = "",
    accentColor: Color,
    compact: Boolean
) {
    val textSize = if (compact) 18.sp else 24.sp
    when {
        titleLower.contains("chatgpt") -> {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Default.AutoAwesome, contentDescription = "OpenAI", tint = accentColor, modifier = Modifier.size(if (compact) 20.dp else 26.dp))
                Text("GPT", color = accentColor, fontSize = textSize, fontWeight = FontWeight.Black)
            }
        }
        titleLower.contains("google ai") || titleLower.contains("gemini") -> {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("✦", color = Color(0xFF60A5FA), fontSize = textSize, fontWeight = FontWeight.Black)
                Text("G", color = Color.White, fontSize = textSize, fontWeight = FontWeight.Black)
                Text("AI", color = Color(0xFFC084FC), fontSize = textSize, fontWeight = FontWeight.Black)
            }
        }
        titleLower.contains("gmail") -> {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Default.Email, contentDescription = "Gmail", tint = Color(0xFFEA4335), modifier = Modifier.size(if (compact) 20.dp else 26.dp))
                Text("M", color = Color.White, fontSize = textSize, fontWeight = FontWeight.Black)
            }
        }
        titleLower.contains("google storage") || titleLower.contains("google one") -> {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Default.CloudDone, contentDescription = "Google One", tint = Color(0xFF34A853), modifier = Modifier.size(if (compact) 20.dp else 26.dp))
                Text(if (titleLower.contains("5tb")) "5TB" else "2TB", color = Color.White, fontSize = textSize, fontWeight = FontWeight.Black)
            }
        }
        titleLower.contains("grok") -> {
            Text("𝕏 / Grok", color = Color.White, fontSize = if (compact) 15.sp else 20.sp, fontWeight = FontWeight.Black)
        }
        titleLower.contains("claude") -> {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("✻", color = Color(0xFFD97757), fontSize = textSize, fontWeight = FontWeight.Black)
                Text("Claude", color = Color.White, fontSize = if (compact) 14.sp else 18.sp, fontWeight = FontWeight.Bold)
            }
        }
        titleLower.contains("perplexity") -> {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Default.TravelExplore, contentDescription = "Perplexity", tint = accentColor, modifier = Modifier.size(if (compact) 20.dp else 26.dp))
                Text("PPLX", color = Color.White, fontSize = if (compact) 14.sp else 18.sp, fontWeight = FontWeight.Black)
            }
        }
        titleLower.contains("cursor") -> {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Default.Terminal, contentDescription = "Cursor", tint = accentColor, modifier = Modifier.size(if (compact) 20.dp else 26.dp))
                Text("{ }", color = accentColor, fontSize = if (compact) 15.sp else 20.sp, fontWeight = FontWeight.Black)
            }
        }
        titleLower.contains("lovable") -> {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Default.Favorite, contentDescription = "Lovable", tint = Color(0xFFEC4899), modifier = Modifier.size(if (compact) 20.dp else 26.dp))
                Text("LVB", color = Color.White, fontSize = if (compact) 15.sp else 19.sp, fontWeight = FontWeight.Black)
            }
        }
        titleLower.contains("capcut") -> {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Default.ContentCut, contentDescription = "CapCut", tint = accentColor, modifier = Modifier.size(if (compact) 20.dp else 26.dp))
                Text("PRO", color = Color.White, fontSize = if (compact) 14.sp else 18.sp, fontWeight = FontWeight.Black)
            }
        }
        titleLower.contains("pdf") || titleLower.contains("acrobat") -> {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Default.PictureAsPdf, contentDescription = "PDF", tint = accentColor, modifier = Modifier.size(if (compact) 20.dp else 26.dp))
                Text("PDF", color = Color.White, fontSize = if (compact) 14.sp else 18.sp, fontWeight = FontWeight.Black)
            }
        }
        titleLower.contains("adobe") -> {
            Text("🅰 CC", color = Color(0xFFFF3B30), fontSize = textSize, fontWeight = FontWeight.Black)
        }
        titleLower.contains("canva") -> {
            Text("Canva", color = Color(0xFF00C4CC), fontSize = if (compact) 16.sp else 22.sp, fontWeight = FontWeight.ExtraBold)
        }
        titleLower.contains("youtube") -> {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Default.PlayCircleFilled, contentDescription = "YouTube", tint = Color(0xFFFF0033), modifier = Modifier.size(if (compact) 22.dp else 28.dp))
                Text("YT", color = Color.White, fontSize = if (compact) 15.sp else 20.sp, fontWeight = FontWeight.Black)
            }
        }
        titleLower.contains("spotify") -> {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Default.GraphicEq, contentDescription = "Spotify", tint = Color(0xFF1DB954), modifier = Modifier.size(if (compact) 22.dp else 28.dp))
                Text("♫", color = Color(0xFF1DB954), fontSize = textSize, fontWeight = FontWeight.Black)
            }
        }
        titleLower.contains("netflix") -> {
            Text("N • 4K", color = Color(0xFFE50914), fontSize = if (compact) 15.sp else 20.sp, fontWeight = FontWeight.Black)
        }
        titleLower.contains("windows") -> {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Default.Window, contentDescription = "Windows 11", tint = Color(0xFF00A4EF), modifier = Modifier.size(if (compact) 20.dp else 26.dp))
                Text("Win 11", color = Color.White, fontSize = if (compact) 14.sp else 18.sp, fontWeight = FontWeight.Black)
            }
        }
        titleLower.contains("microsoft 365") || titleLower.contains("office") -> {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Default.Description, contentDescription = "Office", tint = accentColor, modifier = Modifier.size(if (compact) 20.dp else 26.dp))
                Text(if (titleLower.contains("365")) "M365" else "Office", color = Color.White, fontSize = if (compact) 14.sp else 18.sp, fontWeight = FontWeight.Black)
            }
        }
        categoryLower.contains("ეკრან") -> {
            Icon(Icons.Default.Smartphone, contentDescription = null, tint = accentColor, modifier = Modifier.size(if (compact) 20.dp else 26.dp))
        }
        categoryLower.contains("ელემენტ") -> {
            Icon(Icons.Default.BatteryChargingFull, contentDescription = null, tint = accentColor, modifier = Modifier.size(if (compact) 20.dp else 26.dp))
        }
        categoryLower.contains("კამერ") -> {
            Icon(Icons.Default.CameraAlt, contentDescription = null, tint = accentColor, modifier = Modifier.size(if (compact) 20.dp else 26.dp))
        }
        else -> {
            Icon(Icons.Default.Verified, contentDescription = null, tint = accentColor, modifier = Modifier.size(if (compact) 20.dp else 26.dp))
        }
    }
}

