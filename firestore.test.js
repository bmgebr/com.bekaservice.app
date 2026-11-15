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

  // Alice can also register a second device token under ${ALICE_UID}_${deviceId} and delete it on logout
  const aliceSecondDeviceDocId = `${ALICE_UID}_dev_tablet_2`;
  await assertSucceeds(
    aliceDb.collection("fcm_tokens").doc(aliceSecondDeviceDocId).set({
      ...sampleTokenDoc,
      deviceId: "dev_tablet_2",
      token: "fcm_token_alice_second_device_tablet_1234567890",
    })
  );
  // Bob CANNOT read, overwrite, or delete Alice's second device token
  await assertFails(bobDb.collection("fcm_tokens").doc(aliceSecondDeviceDocId).get());
  await assertFails(bobDb.collection("fcm_tokens").doc(aliceSecondDeviceDocId).delete());
  await assertSucceeds(aliceDb.collection("fcm_tokens").doc(aliceSecondDeviceDocId).delete());

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
    title: "🔥 ახალი ფასდაკლება BSTORE-ში!",
    body: "ChatGPT Plus და iPhone ეკრანების შეცვლა სპეციალურ ფასად.",
    type: "product_offer",
    visibility: "PUBLIC",
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

  // 4. Verified sole admin (beqa1994@gmail.com, email_verified: true) CAN send PUBLIC notification
  await assertSucceeds(
    verifiedBekaDb.collection("notifications").doc("notif_promo_1").set(validNotification)
  );

  // 5. Public read & PUBLIC query allowed; unfiltered collection scan rejected for non-admin
  await assertSucceeds(unauthDb.collection("notifications").doc("notif_promo_1").get());
  await assertSucceeds(
    unauthDb.collection("notifications").where("visibility", "==", "PUBLIC").get()
  );
  await assertSucceeds(
    aliceDb.collection("notifications").where("visibility", "==", "PUBLIC").get()
  );
  await assertFails(aliceDb.collection("notifications").get());
  await assertFails(unauthDb.collection("notifications").get());

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

test("Retired IMEI collections lockdown: client writes blocked, only verified sole admin can read historical records", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const verifiedBekaDb = testEnv
    .authenticatedContext("beka_verified_uid", {
      email: "beqa1994@gmail.com",
      email_verified: true,
    })
    .firestore();

  const now = new Date(Date.now() - 1000);

  // Seed historical records via admin bypass
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("imei_brands").doc("apple").set({
      id: "apple",
      name: "Apple",
      displayOrder: 1,
      enabled: true,
      updatedAt: now,
    });
    await context.firestore().collection("imei_requests").doc("hist_req_1").set({
      id: "hist_req_1",
      userId: ALICE_UID,
      status: "completed",
      createdAt: now,
    });
  });

  // Client writes are strictly blocked for all users
  await assertFails(
    aliceDb.collection("imei_brands").doc("apple").set({
      id: "apple",
      name: "Apple",
    })
  );
  await assertFails(
    verifiedBekaDb.collection("imei_brands").doc("apple").set({
      id: "apple",
      name: "Apple",
    })
  );
  await assertFails(
    aliceDb.collection("imei_requests").doc("new_req").set({
      id: "new_req",
      userId: ALICE_UID,
    })
  );

  // Unauthenticated and ordinary users cannot read retired IMEI collections; verified sole admin can read
  await assertFails(unauthDb.collection("imei_requests").doc("hist_req_1").get());
  await assertFails(aliceDb.collection("imei_requests").doc("hist_req_1").get());
  await assertSucceeds(verifiedBekaDb.collection("imei_requests").doc("hist_req_1").get());
  await assertSucceeds(verifiedBekaDb.collection("imei_brands").doc("apple").get());
});

test("Admin-Only Order Notifications & FCM Tokens: only beqa1994@gmail.com can read/update admin_notifications and register admin_fcm_tokens; customers cannot read or mark as read", async () => {
  const verifiedBekaDb = testEnv
    .authenticatedContext("beka_verified_uid", {
      email: "beqa1994@gmail.com",
      email_verified: true,
      admin: true,
    })
    .firestore();
  const aliceDb = testEnv
    .authenticatedContext(ALICE_UID, {
      email: "alice@example.com",
      email_verified: true,
    })
    .firestore();
  const bobDb = testEnv
    .authenticatedContext(BOB_UID, {
      email: "bob@example.com",
      email_verified: true,
    })
    .firestore();

  const now = new Date(Date.now() - 1000);

  // 1. Admin device token registration: ONLY verified sole admin can register/read/delete in /admin_fcm_tokens
  await assertSucceeds(
    verifiedBekaDb.collection("admin_fcm_tokens").doc("token_device_1").set({
      tokenId: "token_device_1",
      token: "fcm_admin_token_device_1_valid_string_12345",
      uid: "beka_verified_uid",
      email: "beqa1994@gmail.com",
      deviceId: "pixel_9_pro_admin_1",
      deviceModel: "Google Pixel 9 Pro",
      active: true,
      updatedAt: now,
    })
  );
  await assertSucceeds(
    verifiedBekaDb.collection("admin_fcm_tokens").doc("token_device_2").set({
      tokenId: "token_device_2",
      token: "fcm_admin_token_device_2_valid_string_67890",
      uid: "beka_verified_uid",
      email: "beqa1994@gmail.com",
      deviceId: "galaxy_s25_ultra_admin_2",
      deviceModel: "Samsung Galaxy S25 Ultra",
      active: true,
      updatedAt: now,
    })
  );

  // Customer Alice CANNOT register an admin FCM token or read/delete existing admin tokens
  await assertFails(
    aliceDb.collection("admin_fcm_tokens").doc("token_alice_hack").set({
      tokenId: "token_alice_hack",
      token: "fcm_alice_token_1234567890",
      uid: ALICE_UID,
      email: "beqa1994@gmail.com",
      deviceId: "alice_phone",
      active: true,
      updatedAt: now,
    })
  );
  await assertFails(aliceDb.collection("admin_fcm_tokens").doc("token_device_1").get());
  await assertFails(aliceDb.collection("admin_fcm_tokens").doc("token_device_1").delete());

  // 2. Alice creates her own valid service order and corresponding sanitized admin notification
  const reqId = "service_ord_notif_101";
  await assertSucceeds(
    aliceDb.collection("orders").doc(reqId).set({
      id: reqId,
      userId: ALICE_UID,
      userName: "Alice Customer",
      userEmail: "alice@example.com",
      userPhone: "599112233",
      deviceModel: "iPhone 15 Pro",
      serviceType: "ეკრანის შეცვლა",
      status: "pending",
      priceVerified: false,
      adminRead: false,
      createdAt: now,
      updatedAt: now,
    })
  );

  const notifDocId = `admin_order_${reqId}`;
  await assertSucceeds(
    aliceDb.collection("admin_notifications").doc(notifDocId).set({
      id: notifDocId,
      orderId: reqId,
      orderType: "service_order",
      title: "ახალი შეკვეთა",
      body: "შემოვიდა ახალი სერვისის შეკვეთა.",
      deepLink: `bekaservice://admin/orders/service/${reqId}`,
      targetRecipientEmail: "beqa1994@gmail.com",
      createdByUid: ALICE_UID,
      read: false,
      createdAtMillis: Date.now() - 1000,
      createdAt: now,
    })
  );

  // 3. Alice CANNOT create a pre-read notification (read: true) or a notification for an order she does not own
  await assertFails(
    bobDb.collection("admin_notifications").doc(`admin_order_${reqId}`).set({
      id: `admin_order_${reqId}`,
      orderId: reqId,
      orderType: "service_order",
      title: "ახალი შეკვეთა",
      body: "შემოვიდა ახალი სერვისის შეკვეთა.",
      deepLink: `bekaservice://admin/orders/service/${reqId}`,
      targetRecipientEmail: "beqa1994@gmail.com",
      createdByUid: BOB_UID,
      read: false,
      createdAt: now,
    })
  );

  // 4. Customers (Alice and Bob) CANNOT read, list, update (mark as read), or delete /admin_notifications
  await assertFails(aliceDb.collection("admin_notifications").doc(notifDocId).get());
  await assertFails(bobDb.collection("admin_notifications").doc(notifDocId).get());
  await assertFails(
    aliceDb.collection("admin_notifications").doc(notifDocId).update({
      read: true,
    })
  );
  await assertFails(aliceDb.collection("admin_notifications").doc(notifDocId).delete());

  // 5. ONLY verified sole admin (beqa1994@gmail.com) can read and mark /admin_notifications as read
  await assertSucceeds(verifiedBekaDb.collection("admin_notifications").doc(notifDocId).get());
  await assertSucceeds(
    verifiedBekaDb.collection("admin_notifications").doc(notifDocId).update({
      read: true,
      readAtMillis: Date.now() - 200,
      readByEmail: "beqa1994@gmail.com",
      readAt: new Date(Date.now() - 200),
    })
  );

  // Admin can also clean up expired/invalid FCM tokens
  await assertSucceeds(
    verifiedBekaDb.collection("admin_fcm_tokens").doc("token_device_2").delete()
  );
});

test("Sole admin custom claims & /admins/{uid} synchronization: verified beqa1994@gmail.com self-sync succeeds, ordinary/unauthenticated/unverified/disabled users are rejected", async () => {
  const now = new Date(Date.now() - 1000);
  const freshBekaUid = "beka_fresh_login_uid";

  const verifiedBekaDb = testEnv
    .authenticatedContext(freshBekaUid, {
      email: "beqa1994@gmail.com",
      email_verified: true,
    })
    .firestore();

  const unverifiedBekaDb = testEnv
    .authenticatedContext(freshBekaUid, {
      email: "beqa1994@gmail.com",
      email_verified: false,
    })
    .firestore();

  const aliceDb = testEnv
    .authenticatedContext(ALICE_UID, {
      email: "alice@example.com",
      email_verified: true,
    })
    .firestore();

  const unauthDb = testEnv.unauthenticatedContext().firestore();

  // 1. Unauthenticated user cannot create or read /admins/{uid}
  await assertFails(
    unauthDb.collection("admins").doc(freshBekaUid).set({
      uid: freshBekaUid,
      email: "beqa1994@gmail.com",
      enabled: true,
      role: "sole_admin",
      customClaimAdmin: true,
      updatedAt: now,
    })
  );

  // 2. Ordinary authenticated user (Alice) CANNOT create /admins/{aliceUid} even if spoofing email: "beqa1994@gmail.com"
  await assertFails(
    aliceDb.collection("admins").doc(ALICE_UID).set({
      uid: ALICE_UID,
      email: "beqa1994@gmail.com",
      enabled: true,
      role: "sole_admin",
      customClaimAdmin: true,
      updatedAt: now,
    })
  );

  // Ordinary authenticated user (Alice) CANNOT create /admins/{freshBekaUid}
  await assertFails(
    aliceDb.collection("admins").doc(freshBekaUid).set({
      uid: freshBekaUid,
      email: "beqa1994@gmail.com",
      enabled: true,
      role: "sole_admin",
      customClaimAdmin: true,
      updatedAt: now,
    })
  );

  // 3. Unverified beqa1994@gmail.com (email_verified: false) CANNOT self-sync /admins/{freshBekaUid}
  await assertFails(
    unverifiedBekaDb.collection("admins").doc(freshBekaUid).set({
      uid: freshBekaUid,
      email: "beqa1994@gmail.com",
      enabled: true,
      role: "sole_admin",
      customClaimAdmin: true,
      updatedAt: now,
    })
  );

  // 4. Verified sole admin (beqa1994@gmail.com, email_verified: true) CAN self-sync own /admins/{freshBekaUid}
  await assertSucceeds(
    verifiedBekaDb.collection("admins").doc(freshBekaUid).set({
      uid: freshBekaUid,
      email: "beqa1994@gmail.com",
      enabled: true,
      role: "sole_admin",
      customClaimAdmin: true,
      updatedAt: now,
    })
  );

  // Verified sole admin CANNOT create an /admins document for another user's UID
  await assertFails(
    verifiedBekaDb.collection("admins").doc(ALICE_UID).set({
      uid: ALICE_UID,
      email: "beqa1994@gmail.com",
      enabled: true,
      role: "sole_admin",
      customClaimAdmin: true,
      updatedAt: now,
    })
  );

  // Nobody can delete an /admins document from the client
  await assertFails(
    verifiedBekaDb.collection("admins").doc(freshBekaUid).delete()
  );

  // 5. If /admins/{uid} is explicitly disabled on the server (enabled: false), client cannot re-enable it
  const revokedBekaUid = "beka_revoked_uid";
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("admins").doc(revokedBekaUid).set({
      uid: revokedBekaUid,
      email: "beqa1994@gmail.com",
      enabled: false,
      role: "sole_admin",
      updatedAt: now,
    });
  });

  const revokedBekaDb = testEnv
    .authenticatedContext(revokedBekaUid, {
      email: "beqa1994@gmail.com",
      email_verified: true,
    })
    .firestore();

  await assertFails(
    revokedBekaDb.collection("admins").doc(revokedBekaUid).set({
      uid: revokedBekaUid,
      email: "beqa1994@gmail.com",
      enabled: true,
      role: "sole_admin",
      customClaimAdmin: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("BSTORE Payment System: Bank Transfer (BOG/TBC) & COD orders, bank accounts access, payment confirmation, and immutable audit logs", async () => {
  const now = new Date(Date.now() - 1000);
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const aliceDb = testEnv
    .authenticatedContext(ALICE_UID, {
      email: "alice@example.com",
      email_verified: true,
    })
    .firestore();
  const bobDb = testEnv
    .authenticatedContext(BOB_UID, {
      email: "bob@example.com",
      email_verified: true,
    })
    .firestore();
  const verifiedBekaDb = testEnv
    .authenticatedContext(ADMIN_UID, {
      email: "beqa1994@gmail.com",
      email_verified: true,
      admin: true,
    })
    .firestore();

  // 1. /payment_bank_accounts: unauthenticated cannot read; non-admin cannot write; admin can write valid Georgian IBAN
  const bogAccount = {
    id: "bank_bog_primary",
    bankCode: "bog",
    bankName: "საქართველოს ბანკი",
    recipientName: "BSTORE / ბექა მღებრიშვილი",
    iban: "GE17BG0000000580620970",
    currency: "GEL",
    enabled: true,
    sortOrder: 1,
    paymentInstructions: "დანიშნულებაში მიუთითეთ შეკვეთის ნომერი.",
    updatedAtMillis: Date.now() - 1000,
    updatedAt: now,
  };

  await assertFails(
    aliceDb.collection("payment_bank_accounts").doc("bank_bog_primary").set(bogAccount)
  );
  await assertSucceeds(
    verifiedBekaDb.collection("payment_bank_accounts").doc("bank_bog_primary").set(bogAccount)
  );

  // Invalid IBAN rejected even for admin
  await assertFails(
    verifiedBekaDb.collection("payment_bank_accounts").doc("bank_invalid").set({
      ...bogAccount,
      id: "bank_invalid",
      iban: "INVALID_IBAN_123",
    })
  );

  // Unauthenticated user cannot read bank accounts; authenticated customer can read
  await assertFails(unauthDb.collection("payment_bank_accounts").doc("bank_bog_primary").get());
  await assertSucceeds(aliceDb.collection("payment_bank_accounts").doc("bank_bog_primary").get());

  // 2. Customer creates Bank of Georgia transfer order (paymentStatus: 'pending') -> succeeds
  const bogOrderId = "ord_bog_1001";
  await assertSucceeds(
    aliceDb.collection("orders").doc(bogOrderId).set({
      id: bogOrderId,
      orderNumber: "#BS-1001",
      userId: ALICE_UID,
      userName: "Alice",
      userPhone: "+995555112233",
      userEmail: "alice@example.com",
      deliveryAddress: "თბილისი, ჭავჭავაძის 10",
      deliveryMethodCode: "courier",
      deliveryCity: "თბილისი",
      deliveryFeeText: "საკურიერო ტარიფი ქალაქის მიხედვით",
      deviceModel: "iPhone 16 Pro OLED",
      serviceType: "Order #BS-1001",
      status: "pending",
      price: "390 ₾",
      totalAmount: 390,
      priceVerified: false,
      currency: "GEL",
      paymentMethod: "საბანკო გადარიცხვა (საქართველოს ბანკი)",
      paymentMethodCode: "bank_transfer",
      paymentStatus: "pending",
      bankAccountId: "bank_bog_primary",
      bankCode: "bog",
      bankName: "საქართველოს ბანკი",
      recipientNameSnapshot: "BSTORE / ბექა მღებრიშვილი",
      ibanSnapshot: "GE17BG0000000580620970",
      paymentReference: "#BS-1001",
      paymentInstructionsSnapshot: "დანიშნულებაში მიუთითეთ #BS-1001",
      paymentConfirmedAtMillis: 0,
      paymentConfirmedBy: "",
      paymentInternalNote: "",
      acceptedTermsVersion: "2026.11-BSTORE-v1",
      acceptedTermsAtMillis: Date.now() - 1000,
      trackingCode: "BS-TRK-1001",
      adminRead: false,
      createdAt: now,
      updatedAt: now,
    })
  );

  // 3. Customer creates Cash on Delivery (COD) order (paymentStatus: 'awaiting_collection') -> succeeds
  const codOrderId = "ord_cod_1002";
  await assertSucceeds(
    aliceDb.collection("orders").doc(codOrderId).set({
      id: codOrderId,
      orderNumber: "#BS-1002",
      userId: ALICE_UID,
      userName: "Alice",
      userPhone: "+995555112233",
      userEmail: "alice@example.com",
      deliveryAddress: "ახალციხე, კოსტავას N10",
      deliveryMethodCode: "pickup",
      deliveryCity: "ახალციხე",
      deliveryFeeText: "0 ₾ (უფასო)",
      deviceModel: "Samsung S24 Battery",
      serviceType: "Order #BS-1002",
      status: "pending",
      price: "120 ₾",
      totalAmount: 120,
      priceVerified: false,
      currency: "GEL",
      paymentMethod: "ნაღდი ანგარიშსწორება (მიღებისას)",
      paymentMethodCode: "cod",
      paymentStatus: "awaiting_collection",
      paymentReference: "#BS-1002",
      paymentConfirmedAtMillis: 0,
      paymentConfirmedBy: "",
      acceptedTermsVersion: "2026.11-BSTORE-v1",
      acceptedTermsAtMillis: Date.now() - 1000,
      trackingCode: "BS-TRK-1002",
      adminRead: false,
      createdAt: now,
      updatedAt: now,
    })
  );

  // 4. Customer CANNOT create an order with paymentStatus == 'paid'
  await assertFails(
    aliceDb.collection("orders").doc("ord_forged_paid").set({
      id: "ord_forged_paid",
      userId: ALICE_UID,
      userPhone: "+995555112233",
      deviceModel: "iPhone 16 Pro OLED",
      serviceType: "Order #BS-9999",
      status: "pending",
      paymentMethodCode: "bank_transfer",
      paymentStatus: "paid",
      priceVerified: false,
      createdAt: now,
    })
  );

  // 5. Customer CANNOT mark their own existing order as paid
  await assertFails(
    aliceDb.collection("orders").doc(bogOrderId).update({
      paymentStatus: "paid",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // 6. Another customer (Bob) CANNOT read Alice's order or payment instructions
  await assertFails(bobDb.collection("orders").doc(bogOrderId).get());

  // 7. Verified Admin CAN confirm payment on Alice's order and append an immutable audit log
  await assertSucceeds(
    verifiedBekaDb.collection("orders").doc(bogOrderId).update({
      paymentStatus: "paid",
      paymentConfirmedAtMillis: Date.now() - 300,
      paymentConfirmedBy: "beqa1994@gmail.com",
      paymentInternalNote: "BOG ჩარიცხვა შემოწმებულია",
      priceVerified: true,
      updatedAt: new Date(Date.now() - 300),
    })
  );

  const auditId = "audit_1001_bog";
  await assertFails(
    aliceDb.collection("payment_audit_logs").doc(auditId).set({
      id: auditId,
      orderId: bogOrderId,
      orderNumber: "#BS-1001",
      eventType: "BANK_TRANSFER_CONFIRMED",
      previousStatus: "pending",
      newStatus: "paid",
      amount: "390 ₾",
      currency: "GEL",
      actorUid: ALICE_UID,
      actorEmail: "beqa1994@gmail.com",
      actorRole: "sole_admin",
      timestamp: Date.now() - 300,
      note: "Forged audit",
    })
  );

  await assertSucceeds(
    verifiedBekaDb.collection("payment_audit_logs").doc(auditId).set({
      id: auditId,
      orderId: bogOrderId,
      orderNumber: "#BS-1001",
      eventType: "BANK_TRANSFER_CONFIRMED",
      previousStatus: "pending",
      newStatus: "paid",
      amount: "390 ₾",
      currency: "GEL",
      actorUid: ADMIN_UID,
      actorEmail: "beqa1994@gmail.com",
      actorRole: "sole_admin",
      timestamp: Date.now() - 300,
      note: "BOG ჩარიცხვა შემოწმებულია",
    })
  );

  // Audit log is strictly immutable: even admin cannot update or delete an existing audit log
  await assertFails(
    verifiedBekaDb.collection("payment_audit_logs").doc(auditId).update({
      note: "Tampered note",
    })
  );
  await assertFails(
    verifiedBekaDb.collection("payment_audit_logs").doc(auditId).delete()
  );

  // 8. Customer CAN switch bank while pending and mark transfer sent (verification_pending),
  //    but CANNOT switch after verification_pending, CANNOT alter price/totalAmount, and CANNOT mark paid
  const pendingTransferOrderId = "ord_tbc_pending_2001";
  await assertSucceeds(
    aliceDb.collection("orders").doc(pendingTransferOrderId).set({
      id: pendingTransferOrderId,
      orderNumber: "#BS-2001",
      userId: ALICE_UID,
      userName: "Alice",
      userPhone: "+995555112233",
      userEmail: "alice@example.com",
      deviceModel: "iPhone 15 OLED",
      serviceType: "Order #BS-2001",
      status: "pending",
      price: "250 ₾",
      totalAmount: 250,
      priceVerified: false,
      currency: "GEL",
      paymentMethod: "საბანკო გადარიცხვა (საქართველოს ბანკი)",
      paymentMethodCode: "bank_transfer",
      paymentStatus: "pending",
      bankAccountId: "bank_bog_primary",
      bankCode: "bog",
      bankName: "საქართველოს ბანკი",
      recipientNameSnapshot: "BSTORE",
      ibanSnapshot: "GE17BG0000000580620970",
      paymentReference: "#BS-2001",
      userConfirmedPaymentAtMillis: 0,
      paymentConfirmedAtMillis: 0,
      paymentConfirmedBy: "",
      adminRead: false,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Admin notification with order number & summary succeeds; PII email in notification body fails
  await assertFails(
    aliceDb.collection("admin_notifications").doc(`admin_order_${pendingTransferOrderId}`).set({
      id: `admin_order_${pendingTransferOrderId}`,
      orderId: pendingTransferOrderId,
      orderType: "service_order",
      title: "ახალი შეკვეთა #BS-2001",
      body: "alice@example.com შეკვეთა",
      deepLink: `bekaservice://admin/orders/service/${pendingTransferOrderId}`,
      createdByUid: ALICE_UID,
      read: false,
      createdAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("admin_notifications").doc(`admin_order_${pendingTransferOrderId}`).set({
      id: `admin_order_${pendingTransferOrderId}`,
      orderId: pendingTransferOrderId,
      orderType: "service_order",
      title: "ახალი შეკვეთა #BS-2001",
      body: "ახალი შეკვეთა #BS-2001 • iPhone 15 OLED • 250 ₾",
      deepLink: `bekaservice://admin/orders/service/${pendingTransferOrderId}`,
      targetRecipientEmail: "beqa1994@gmail.com",
      createdByUid: ALICE_UID,
      read: false,
      createdAtMillis: Date.now() - 500,
      createdAt: now,
    })
  );

  // Switch bank while pending succeeds
  await assertSucceeds(
    aliceDb.collection("orders").doc(pendingTransferOrderId).update({
      bankAccountId: "bank_tbc_primary",
      bankCode: "tbc",
      bankName: "თიბისი ბანკი",
      ibanSnapshot: "GE73TB7114445061100057",
      paymentMethod: "საბანკო გადარიცხვა (თიბისი ბანკი)",
      updatedAt: new Date(Date.now() - 300),
    })
  );

  // Changing totalAmount alongside verification_pending fails
  await assertFails(
    aliceDb.collection("orders").doc(pendingTransferOrderId).update({
      paymentStatus: "verification_pending",
      userConfirmedPaymentAtMillis: Date.now() - 200,
      totalAmount: 1,
      updatedAt: new Date(Date.now() - 200),
    })
  );

  // Confirm transfer sent (verification_pending) succeeds for owner
  await assertSucceeds(
    aliceDb.collection("orders").doc(pendingTransferOrderId).update({
      paymentStatus: "verification_pending",
      userConfirmedPaymentAtMillis: Date.now() - 200,
      updatedAt: new Date(Date.now() - 200),
    })
  );

  // Owner updates own unread admin notification for transfer sent (read must stay false)
  await assertSucceeds(
    aliceDb.collection("admin_notifications").doc(`admin_order_${pendingTransferOrderId}`).update({
      title: "გადარიცხვა დადასტურდა #BS-2001",
      body: "მომხმარებელმა დაადასტურა #BS-2001 შეკვეთის თანხის გადმორიცხვა. საჭიროა საბანკო ანგარიშზე შემოწმება.",
      read: false,
      updatedAt: new Date(Date.now() - 200),
    })
  );

  // Owner CANNOT mark transfer admin notification as read
  await assertFails(
    aliceDb.collection("admin_notifications").doc(`admin_order_${pendingTransferOrderId}`).update({
      read: true,
    })
  );

  // Owner CANNOT mark paymentStatus == 'paid' after verification_pending
  await assertFails(
    aliceDb.collection("orders").doc(pendingTransferOrderId).update({
      paymentStatus: "paid",
      updatedAt: new Date(Date.now() - 100),
    })
  );

  // Verified admin confirms paid and creates targeted customer PRIVATE_USER notification with orderId & recipientUid
  await assertSucceeds(
    verifiedBekaDb.collection("notifications").doc(`notif_order_paid_${pendingTransferOrderId}`).set({
      id: `notif_order_paid_${pendingTransferOrderId}`,
      title: "გადახდა დადასტურდა #BS-2001",
      body: "თქვენი #BS-2001 შეკვეთის გადახდა დადასტურდა.",
      type: "order_update",
      visibility: "PRIVATE_USER",
      recipientUid: ALICE_UID,
      relatedEntityId: pendingTransferOrderId,
      targetTab: 4,
      targetProductId: 0,
      orderId: pendingTransferOrderId,
      orderNumber: "#BS-2001",
      senderEmail: "beqa1994@gmail.com",
      audience: `user:${ALICE_UID}`,
      status: "sent",
      isRead: false,
      createdAt: new Date(Date.now() - 100),
      updatedAt: new Date(Date.now() - 100),
    })
  );
});

test("Notification Privacy Matrix: PRIVATE_USER, ADMIN_ONLY, PUBLIC validation, anonymous auth blocking & FCM ownership", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const anonDb = testEnv
    .authenticatedContext("anon_guest_uid", {
      firebase: { sign_in_provider: "anonymous" },
    })
    .firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const verifiedBekaDb = testEnv
    .authenticatedContext("beka_verified_uid", {
      email: "beqa1994@gmail.com",
      email_verified: true,
    })
    .firestore();

  // 1. PUBLIC notifications MUST NOT carry orderId/recipientUid or private type
  await assertFails(
    verifiedBekaDb.collection("notifications").doc("bad_pub_order").set({
      id: "bad_pub_order",
      title: "Order Leakage Attempt",
      body: "Should fail",
      type: "order_update",
      visibility: "PUBLIC",
      orderId: "ord_123",
      audience: "all",
      createdAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(
    verifiedBekaDb.collection("notifications").doc("bad_pub_recipient").set({
      id: "bad_pub_recipient",
      title: "Recipient Leakage Attempt",
      body: "Should fail",
      type: "announcement",
      visibility: "PUBLIC",
      recipientUid: ALICE_UID,
      audience: "all",
      createdAt: new Date(Date.now() - 500),
    })
  );
  // Missing visibility must be rejected
  await assertFails(
    verifiedBekaDb.collection("notifications").doc("missing_visibility").set({
      id: "missing_visibility",
      title: "No Visibility",
      body: "Should fail",
      type: "announcement",
      audience: "all",
      createdAt: new Date(Date.now() - 500),
    })
  );

  // 2. Admin creates Alice's PRIVATE_USER payment confirmation + ADMIN_ONLY doc + PUBLIC announcement
  const alicePrivateId = "priv_alice_ord_1";
  await assertSucceeds(
    verifiedBekaDb.collection("notifications").doc(alicePrivateId).set({
      id: alicePrivateId,
      title: "გადახდა დადასტურდა",
      body: "შეკვეთა #BS-501 გადახდილია",
      type: "payment_update",
      visibility: "PRIVATE_USER",
      recipientUid: ALICE_UID,
      relatedEntityId: "ord_501",
      orderId: "ord_501",
      orderNumber: "#BS-501",
      targetTab: 4,
      targetProductId: 0,
      senderEmail: "beqa1994@gmail.com",
      audience: `user:${ALICE_UID}`,
      status: "sent",
      isRead: false,
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );

  const adminOnlyId = "admin_alert_1";
  await assertSucceeds(
    verifiedBekaDb.collection("notifications").doc(adminOnlyId).set({
      id: adminOnlyId,
      title: "ახალი გადახდის მოთხოვნა",
      body: "საჭიროებს დადასტურებას",
      type: "admin_order",
      visibility: "ADMIN_ONLY",
      recipientUid: "",
      audience: "admin_only",
      senderEmail: "beqa1994@gmail.com",
      status: "sent",
      createdAt: new Date(Date.now() - 500),
    })
  );

  // 3. Cross-user, guest, and anonymous isolation for PRIVATE_USER & ADMIN_ONLY
  await assertSucceeds(aliceDb.collection("notifications").doc(alicePrivateId).get());
  await assertSucceeds(
    aliceDb.collection("notifications").where("recipientUid", "==", ALICE_UID).get()
  );

  // Bob, unauth, and anonymous CANNOT read Alice's PRIVATE_USER notification or query her UID
  await assertFails(bobDb.collection("notifications").doc(alicePrivateId).get());
  await assertFails(
    bobDb.collection("notifications").where("recipientUid", "==", ALICE_UID).get()
  );
  await assertFails(unauthDb.collection("notifications").doc(alicePrivateId).get());
  await assertFails(
    unauthDb.collection("notifications").where("recipientUid", "==", ALICE_UID).get()
  );
  await assertFails(anonDb.collection("notifications").doc(alicePrivateId).get());
  await assertFails(
    anonDb.collection("notifications").where("recipientUid", "==", ALICE_UID).get()
  );

  // Anonymous auth CANNOT access ADMIN_ONLY or read bank accounts / orders
  await assertFails(bobDb.collection("notifications").doc(adminOnlyId).get());
  await assertFails(anonDb.collection("notifications").doc(adminOnlyId).get());
  await assertFails(unauthDb.collection("notifications").doc(adminOnlyId).get());
  await assertSucceeds(verifiedBekaDb.collection("notifications").doc(adminOnlyId).get());
  await assertFails(anonDb.collection("payment_bank_accounts").get());

  // 4. Alice can mark ONLY isRead/readAt/updatedAt on her own PRIVATE_USER notification
  await assertSucceeds(
    aliceDb.collection("notifications").doc(alicePrivateId).update({
      isRead: true,
      readAt: new Date(Date.now() - 100),
      updatedAt: new Date(Date.now() - 100),
    })
  );
  // Alice cannot tamper with ownership or title
  await assertFails(
    aliceDb.collection("notifications").doc(alicePrivateId).update({
      recipientUid: BOB_UID,
      isRead: true,
    })
  );
  await assertFails(
    aliceDb.collection("notifications").doc(alicePrivateId).update({
      title: "Tampered",
    })
  );
  // Bob cannot mark Alice's notification read
  await assertFails(
    bobDb.collection("notifications").doc(alicePrivateId).update({
      isRead: true,
    })
  );

  // 5. Anonymous Firebase session CANNOT register an FCM token or query fcm_tokens
  await assertFails(
    anonDb.collection("fcm_tokens").doc("anon_guest_uid_dev1").set({
      userId: "anon_guest_uid",
      token: "fake_anon_fcm_token_1234567890",
      deviceId: "dev1",
      active: true,
      updatedAt: new Date(Date.now() - 200),
    })
  );

  // Alice multi-device FCM token registration & owner-scoped listing succeeds, Bob blocked
  await assertSucceeds(
    aliceDb.collection("fcm_tokens").doc(`${ALICE_UID}_dev1`).set({
      userId: ALICE_UID,
      token: "valid_fcm_token_device_1_abcdefghijklmnopqrstuvwxyz",
      deviceId: "dev1",
      active: true,
      notificationsEnabled: true,
      updatedAt: new Date(Date.now() - 200),
    })
  );
  await assertSucceeds(
    aliceDb.collection("fcm_tokens").where("userId", "==", ALICE_UID).get()
  );
  await assertFails(
    bobDb.collection("fcm_tokens").where("userId", "==", ALICE_UID).get()
  );
});



