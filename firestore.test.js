const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";
const ADMIN_UID = "admin_beka_789";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
    await testEnv.withSecurityRulesDisabled(async (context) => {
      await context.firestore().collection("admins").doc(ADMIN_UID).set({
        email: "beqa1994@gmail.com",
        role: "super_admin",
        enabled: true,
      });
    });
  }
});

test("Unauthenticated user: cannot read users collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
});

test("Authenticated user: cannot read another user's profile", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(BOB_UID).set({
      userId: BOB_UID,
      displayName: "Bob",
      email: "bob@example.com",
      createdAt: new Date(),
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("users").doc(BOB_UID).get());
});

test("Authenticated user: can create/update own profile, but CANNOT set or modify balance or isAdmin", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const createdAt = new Date(Date.now() - 1000);

  // Attempting to create profile with balance must fail
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      email: "alice@example.com",
      balance: 9999,
      createdAt: createdAt,
    })
  );

  // Creating valid profile without financial/privilege fields succeeds (including favoriteProductIds)
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      email: "alice@example.com",
      phoneNumber: "+995 598 23 33 32",
      photoUrl: "https://example.com/avatar.jpg",
      favoriteProductIds: [101, 103],
      createdAt: createdAt,
      updatedAt: createdAt,
    })
  );

  // Updating favoriteProductIds on own account succeeds
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      favoriteProductIds: [101, 103, 105],
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Another user (Bob) cannot modify Alice's favoriteProductIds
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({
      favoriteProductIds: [],
      updatedAt: new Date(Date.now() - 300),
    })
  );

  // Attempting to update balance from client must be rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      balance: 500,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Attempting to self-assign isAdmin from client must be rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
    })
  );
});

test("Authenticated user: can create and query own orders (priceVerified: false), cannot set priceVerified: true, read others' orders, or change owner/price", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  // Client cannot mark an order as priceVerified: true upon creation
  await assertFails(
    aliceDb.collection("orders").doc("order_forged").set({
      id: "order_forged",
      userId: ALICE_UID,
      userPhone: "+995555123456",
      deviceModel: "iPhone 15 Pro",
      serviceType: "FRP Bypass",
      price: "1 ₾",
      totalAmount: 1,
      priceVerified: true,
      status: "pending",
      createdAt: new Date(Date.now() - 1000),
    })
  );

  // Creating order with priceVerified: false succeeds
  await assertSucceeds(
    aliceDb.collection("orders").doc("order_1").set({
      id: "order_1",
      userId: ALICE_UID,
      userPhone: "+995555123456",
      deviceModel: "iPhone 15 Pro",
      serviceType: "FRP Bypass",
      price: "150 ₾",
      totalAmount: 150,
      priceVerified: false,
      status: "pending",
      createdAt: new Date(Date.now() - 1000),
    })
  );

  await assertSucceeds(
    aliceDb.collection("orders").where("userId", "==", ALICE_UID).get()
  );

  // Bob cannot read Alice's order
  await assertFails(bobDb.collection("orders").doc("order_1").get());

  // Alice cannot modify order price
  await assertFails(
    aliceDb.collection("orders").doc("order_1").update({
      price: "10 ₾",
      status: "cancelled",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Alice cannot transfer order ownership to Bob
  await assertFails(
    aliceDb.collection("orders").doc("order_1").update({
      userId: BOB_UID,
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("Admin security: verified beqa1994@gmail.com succeeds, unverified beqa1994@gmail.com fails, disabled or missing-enabled /admins/{uid} fails, and other verified emails fail", async () => {
  const verifiedBekaDb = testEnv
    .authenticatedContext("beka_verified_uid", {
      email: "beqa1994@gmail.com",
      email_verified: true,
    })
    .firestore();

  const unverifiedBekaDb = testEnv
    .authenticatedContext("beka_unverified_uid", {
      email: "beqa1994@gmail.com",
      email_verified: false,
    })
    .firestore();

  const otherVerifiedUserDb = testEnv
    .authenticatedContext("other_verified_uid", {
      email: "someone.else@gmail.com",
      email_verified: true,
    })
    .firestore();

  // 1. Verified sole admin (beqa1994@gmail.com, email_verified: true) can create/update products
  await assertSucceeds(
    verifiedBekaDb.collection("products").doc("prod_beka_verified").set({
      productId: 777,
      title: "iPhone 16 Pro Max OLED",
      price: "390 ₾",
      category: "ეკრანები",
      createdAt: new Date(Date.now() - 1000),
    })
  );

  // 2. Unverified beqa1994@gmail.com (email_verified: false) MUST be rejected
  await assertFails(
    unverifiedBekaDb.collection("products").doc("prod_unauth_unverified").set({
      title: "Unauthorized Product",
      price: "100 ₾",
      createdAt: new Date(Date.now() - 1000),
    })
  );

  // 3. Another verified Google account (someone.else@gmail.com) MUST be rejected
  await assertFails(
    otherVerifiedUserDb.collection("products").doc("prod_unauth_other").set({
      title: "Unauthorized Product",
      price: "100 ₾",
      createdAt: new Date(Date.now() - 1000),
    })
  );

  // 4. /admins/{uid} without `enabled: true` (e.g. missing enabled field or enabled: false) MUST be rejected
  const missingEnabledUid = "admin_missing_enabled";
  const disabledAdminUid = "admin_disabled_false";
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("admins").doc(missingEnabledUid).set({
      email: "staff@bekaservice.ge",
      role: "admin",
      // enabled field intentionally omitted
    });
    await context.firestore().collection("admins").doc(disabledAdminUid).set({
      email: "beqa1994@gmail.com",
      role: "super_admin",
      enabled: false,
    });
  });

  const missingEnabledDb = testEnv.authenticatedContext(missingEnabledUid).firestore();
  const disabledAdminDb = testEnv
    .authenticatedContext(disabledAdminUid, {
      email: "beqa1994@gmail.com",
      email_verified: true,
    })
    .firestore();

  await assertFails(
    missingEnabledDb.collection("products").doc("prod_missing_enabled").set({
      title: "Should Fail",
      price: "50 ₾",
      createdAt: new Date(Date.now() - 1000),
    })
  );

  await assertFails(
    disabledAdminDb.collection("products").doc("prod_disabled_admin").set({
      title: "Should Fail",
      price: "50 ₾",
      createdAt: new Date(Date.now() - 1000),
    })
  );
});

test("Products & app_config: public read allowed, non-admin create/update/delete rejected, verified admin full ProductItem create/update/delete allowed", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const adminDb = testEnv.authenticatedContext(ADMIN_UID).firestore();

  const initialCreatedAt = new Date(Date.now() - 2000);

  // Regular user cannot create product
  await assertFails(
    aliceDb.collection("products").doc("prod_1").set({
      productId: 1,
      title: "Fake Product",
      price: "1 ₾",
      rating: "5.0",
      image: "https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?w=600",
      category: "ეკრანები",
      createdAt: initialCreatedAt,
    })
  );

  // Verified Admin (in /admins/{uid}) can create a full ProductItem document
  await assertSucceeds(
    adminDb.collection("products").doc("prod_101").set({
      productId: 101,
      title: "ChatGPT Plus (GPT-4o / o3-mini)",
      price: "65 ₾",
      shortDescription: "OpenAI-ს პრემიუმ პაკეტი",
      description: "სრული წვდომა GPT-4o, o3-mini და DALL-E 3 მოდელებზე.",
      category: "პროგრამული სერვისები",
      subcategory: "AI & პროდუქტიულობა",
      type: "software",
      brand: "OpenAI",
      image: "https://images.unsplash.com/photo-1592899677977-9c10ca588bbd?w=600",
      imageUrl: "https://images.unsplash.com/photo-1592899677977-9c10ca588bbd?w=600",
      inStock: true,
      isFeatured: true,
      displayOrder: 1,
      sortOrder: 1,
      rating: "5.0",
      features: ["GPT-4o ულიმიტო", "Voice Mode"],
      targetAudience: "დეველოპერები, სტუდენტები, ბიზნესი",
      offerType: "ყოველთვიური გამოწერა",
      duration: "1 თვე",
      accountType: "პირადი ანგარიში (Private)",
      activationTerms: "აქტივაცია 15 წუთში",
      officialUrl: "https://openai.com",
      termsUrl: "https://openai.com/terms",
      lastVerifiedAt: "2026-10",
      searchKeywords: "chatgpt openai gpt4",
      compatibleModels: "All Devices",
      warranty: "1 თვე",
      brandColorHex: "#10A37F",
      brandBadgeText: "AI",
      updatedByAdmin: ADMIN_UID,
      createdAt: initialCreatedAt,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Regular user cannot update an existing product's price or stock
  await assertFails(
    aliceDb.collection("products").doc("prod_101").update({
      price: "5 ₾",
      inStock: false,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Regular user cannot delete an existing product
  await assertFails(
    aliceDb.collection("products").doc("prod_101").delete()
  );

  // Verified Admin can update product price, description, availability (inStock), and displayOrder
  await assertSucceeds(
    adminDb.collection("products").doc("prod_101").update({
      price: "55 ₾",
      shortDescription: "განახლებული ფასი და აღწერა",
      inStock: false,
      displayOrder: 2,
      sortOrder: 2,
      updatedByAdmin: ADMIN_UID,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Unauthenticated visitor & regular user can read updated product and app_config
  const snap = await assertSucceeds(unauthDb.collection("products").doc("prod_101").get());
  if (snap.data().price !== "55 ₾" || snap.data().inStock !== false) {
    throw new Error("Updated product fields did not persist in Firestore");
  }

  await assertSucceeds(
    adminDb.collection("app_config").doc("home_banners").set({
      configType: "home_banners",
      banners: [],
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertSucceeds(unauthDb.collection("app_config").doc("home_banners").get());

  // Verified Admin can delete product
  await assertSucceeds(
    adminDb.collection("products").doc("prod_101").delete()
  );
});

test("Admin: can update order status in Admin Panel, cannot change order owner, and can read all users", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const adminDb = testEnv.authenticatedContext(ADMIN_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("orders").doc("order_99").set({
      id: "order_99",
      userId: ALICE_UID,
      userPhone: "+995555123456",
      deviceModel: "ChatGPT Plus",
      serviceType: "Order #BS-99",
      price: "65 ₾",
      totalAmount: 65,
      priceVerified: false,
      status: "pending",
      createdAt: new Date(Date.now() - 1000),
    })
  );

  // Admin can update status (as AdminOrdersManagementView does)
  await assertSucceeds(
    adminDb.collection("orders").doc("order_99").update({
      status: "completed",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Even Admin cannot change the order owner (userId)
  await assertFails(
    adminDb.collection("orders").doc("order_99").update({
      userId: BOB_UID,
      updatedAt: new Date(Date.now() - 200),
    })
  );

  await assertSucceeds(adminDb.collection("users").get());
});

test("FCM Tokens (/fcm_tokens/{userId}): per-user write ownership, no cross-user read/list, admin read/list allowed", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const verifiedBekaDb = testEnv
    .authenticatedContext("beka_verified_uid", {
      email: "beqa1994@gmail.com",
      email_verified: true,
    })
    .firestore();

  const sampleTokenDoc = {
    userId: ALICE_UID,
    token: "fcm_token_alice_device_pixel_9_pro_1234567890",
    email: "alice@example.com",
    deviceModel: "Pixel 9 Pro",
    platform: "android",
    notificationsEnabled: true,
    updatedAt: new Date(Date.now() - 500),
  };

  // Unauthenticated cannot register or read tokens
  await assertFails(unauthDb.collection("fcm_tokens").doc(ALICE_UID).set(sampleTokenDoc));
  await assertFails(unauthDb.collection("fcm_tokens").doc(ALICE_UID).get());

  // Alice can register and update her own FCM token
  await assertSucceeds(aliceDb.collection("fcm_tokens").doc(ALICE_UID).set(sampleTokenDoc));
  await assertSucceeds(
    aliceDb.collection("fcm_tokens").doc(ALICE_UID).update({
      token: "fcm_token_alice_refreshed_9876543210",
      notificationsEnabled: false,
      updatedAt: new Date(Date.now() - 200),
    })
  );

  // Bob CANNOT read, overwrite, or delete Alice's FCM token
  await assertFails(bobDb.collection("fcm_tokens").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("fcm_tokens").doc(ALICE_UID).set({
      ...sampleTokenDoc,
      token: "hijacked_token_1234567890",
    })
  );
  await assertFails(bobDb.collection("fcm_tokens").doc(ALICE_UID).delete());

  // Regular user CANNOT list all FCM tokens
  await assertFails(aliceDb.collection("fcm_tokens").get());

  // Verified sole admin (beqa1994@gmail.com) CAN read and list FCM tokens
  await assertSucceeds(verifiedBekaDb.collection("fcm_tokens").doc(ALICE_UID).get());
  await assertSucceeds(verifiedBekaDb.collection("fcm_tokens").get());
});

test("Push Notifications (/notifications/{notificationId}): only verified beqa1994@gmail.com admin can send/delete, regular & unverified users rejected, public read allowed", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const unverifiedBekaDb = testEnv
    .authenticatedContext("beka_unverified_uid", {
      email: "beqa1994@gmail.com",
      email_verified: false,
    })
    .firestore();
  const verifiedBekaDb = testEnv
    .authenticatedContext("beka_verified_uid", {
      email: "beqa1994@gmail.com",
      email_verified: true,
    })
    .firestore();

  const validNotification = {
    id: "notif_promo_1",
    title: "🔥 ახალი ფასდაკლება BEKASERVICE-ში!",
    body: "ChatGPT Plus და iPhone ეკრანების შეცვლა სპეციალურ ფასად.",
    type: "product_offer",
    targetProductId: 101,
    targetTab: 7,
    senderEmail: "beqa1994@gmail.com",
    audience: "all",
    status: "submitted",
    createdAt: new Date(Date.now() - 500),
    updatedAt: new Date(Date.now() - 500),
  };

  // 1. Unauthenticated user cannot send notification
  await assertFails(
    unauthDb.collection("notifications").doc("notif_unauth").set({
      ...validNotification,
      id: "notif_unauth",
    })
  );

  // 2. Regular authenticated user (Alice) cannot send notification
  await assertFails(
    aliceDb.collection("notifications").doc("notif_alice").set({
      ...validNotification,
      id: "notif_alice",
    })
  );

  // 3. Unverified beqa1994@gmail.com (email_verified: false) cannot send notification
  await assertFails(
    unverifiedBekaDb.collection("notifications").doc("notif_unverified").set({
      ...validNotification,
      id: "notif_unverified",
    })
  );

  // 4. Verified sole admin (beqa1994@gmail.com, email_verified: true) CAN send notification
  await assertSucceeds(
    verifiedBekaDb.collection("notifications").doc("notif_promo_1").set(validNotification)
  );

  // 5. All users (unauthenticated & authenticated) can read and list broadcast notifications
  await assertSucceeds(unauthDb.collection("notifications").doc("notif_promo_1").get());
  await assertSucceeds(aliceDb.collection("notifications").get());

  // 6. Regular user cannot modify or delete admin notification
  await assertFails(
    aliceDb.collection("notifications").doc("notif_promo_1").update({
      title: "Hacked Title",
    })
  );
  await assertFails(
    aliceDb.collection("notifications").doc("notif_promo_1").delete()
  );

  // 7. Verified sole admin CAN update and delete notification
  await assertSucceeds(
    verifiedBekaDb.collection("notifications").doc("notif_promo_1").update({
      title: "განახლებული სათაური",
      status: "updated",
      updatedAt: new Date(Date.now() - 200),
    })
  );
  await assertSucceeds(
    verifiedBekaDb.collection("notifications").doc("notif_promo_1").delete()
  );
});

test("App Update Config (/app_config/updates): public read, admin write with HTTPS validation when published, non-admin rejected", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const verifiedBekaDb = testEnv
    .authenticatedContext("beka_verified_uid", {
      email: "beqa1994@gmail.com",
      email_verified: true,
    })
    .firestore();

  const validUpdateDoc = {
    configType: "updates",
    latestVersion: "2.5.0",
    changeLog: "• დაემატა აპლიკაციის განახლების გვერდი\n• გაუმჯობესდა შეტყობინებების ისტორია",
    downloadUrl: "https://drive.google.com/uc?export=download&id=1AbCdEfGhIjKlMnOpQrStUvWxYz",
    isPublished: true,
    updatedBy: "beqa1994@gmail.com",
    updatedAt: new Date(Date.now() - 500),
  };

  // Non-admin cannot write app_config/updates
  await assertFails(
    aliceDb.collection("app_config").doc("updates").set(validUpdateDoc)
  );

  // Admin cannot publish update with non-HTTPS downloadUrl
  await assertFails(
    verifiedBekaDb.collection("app_config").doc("updates").set({
      ...validUpdateDoc,
      downloadUrl: "http://insecure.example.com/app.apk",
      isPublished: true,
    })
  );

  // Admin CAN publish valid HTTPS update config
  await assertSucceeds(
    verifiedBekaDb.collection("app_config").doc("updates").set(validUpdateDoc)
  );

  // Unauthenticated and regular users can read app_config/updates
  const snap = await assertSucceeds(unauthDb.collection("app_config").doc("updates").get());
  if (snap.data().latestVersion !== "2.5.0" || snap.data().isPublished !== true) {
    throw new Error("App update config did not persist properly");
  }
});


