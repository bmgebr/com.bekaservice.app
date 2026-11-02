/**
 * BEKASERVICE — Trusted Server-Side Admin Authorization & FCM Push Notification Functions
 *
 * Security & Reliability Invariants Enforced by This Backend Module:
 * 1. Named Firestore Database Alignment:
 *    - Targets the application's named Firestore database
 *      ("ai-studio-android-6c10fc50-ae87-4d6f-bd74-277cfbe60b11") so server-side triggers
 *      fire reliably when the Android app is in the foreground, background, or normally closed.
 * 2. Sole Authorized Administrator ("beqa1994@gmail.com"):
 *    - Custom claim { admin: true } and /admins/{uid} document are granted ONLY to the verified
 *      Firebase Auth user whose email is "beqa1994@gmail.com".
 *    - Any other user attempting to claim admin status has their custom claim revoked.
 * 3. Trusted Server-Side Order Notification Triggers (Admin-Only):
 *    - Triggered on new document creation in `imei_requests/{requestId}` and `orders/{orderId}`.
 *    - Creates/updates `/admin_notifications/admin_order_{id}` with a sanitized payload.
 *    - Queries `/admin_fcm_tokens` for active tokens owned by "beqa1994@gmail.com", verifies each
 *      token's UID against Firebase Auth (confirming email === "beqa1994@gmail.com"), rejects any
 *      synthetic placeholder tokens, and sends high-priority combined notification + data FCM
 *      messages EXCLUSIVELY to those verified admin device tokens.
 *    - NEVER sends admin order notifications to customers, regular users, or topic broadcasts.
 *    - NEVER includes the full 15-digit IMEI, customer private information, or sensitive report
 *      data in the FCM push payload.
 * 4. Broadcast & Customer Status Push Triggers (Background & Closed App Delivery):
 *    - `onBroadcastNotificationCreated`: Dispatches high-priority combined notification + data
 *      FCM messages to topic `bekaservice_announcements` when the verified administrator creates
 *      a broadcast notification in `/notifications/{notificationId}`.
 *    - `onImeiReportWritten` & `onServiceOrderStatusUpdated`: Dispatches sanitized high-priority
 *      combined notification + data FCM messages to the order owner's registered device tokens
 *      in `/fcm_tokens` (supports multiple devices per user) and cleans up expired/invalid tokens.
 */

const {
  onDocumentCreated,
  onDocumentWritten,
  onDocumentUpdated,
} = require("firebase-functions/v2/firestore");
const { onCall, HttpsError } = require("firebase-functions/v2/https");
const admin = require("firebase-admin");
const { getFirestore, FieldValue } = require("firebase-admin/firestore");

if (!admin.apps.length) {
  admin.initializeApp();
}

const FIRESTORE_DATABASE_ID =
  process.env.FIRESTORE_DATABASE_ID ||
  "ai-studio-android-6c10fc50-ae87-4d6f-bd74-277cfbe60b11";

const db = getFirestore(FIRESTORE_DATABASE_ID);
const auth = admin.auth();
const messaging = admin.messaging();

const SOLE_AUTHORIZED_ADMIN_EMAIL = "beqa1994@gmail.com";
const BROADCAST_TOPIC = "bekaservice_announcements";
const ADMIN_ORDERS_CHANNEL_ID = "beka_orders_channel";
const GENERAL_PUSH_CHANNEL_ID = "bekaservice_push_channel";
const DEFAULT_TTL_MS = 24 * 60 * 60 * 1000; // 24 hours in milliseconds

/**
 * Normalizes an email address for strict comparison.
 */
function normalizeEmail(email) {
  return typeof email === "string" ? email.trim().toLowerCase() : "";
}

/**
 * Checks whether a token string is a real FCM registration token rather than a local placeholder.
 */
function isRealFcmRegistrationToken(token) {
  if (typeof token !== "string") return false;
  const trimmed = token.trim();
  if (trimmed.length < 20 || trimmed.length > 4096) return false;
  if (trimmed.startsWith("fcm_admin_dev_")) return false;
  return true;
}

/**
 * Strips any accidental 15-digit IMEI sequence, email address, or phone number from text
 * so sensitive customer data can never leak into a push notification payload.
 */
function sanitizeNotificationText(text, fallback) {
  if (typeof text !== "string" || !text.trim()) {
    return fallback;
  }
  return text
    .replace(/\b\d{15}\b/g, "[REDACTED]")
    .replace(/[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}/g, "[REDACTED]")
    .replace(/(?:\+?995[\s-]?)?5\d{2}[\s-]?\d{2}[\s-]?\d{2}[\s-]?\d{2}\b/g, "[REDACTED]")
    .slice(0, 180);
}

/**
 * Verifies with Firebase Auth on the server that the given UID belongs to the
 * sole authorized administrator ("beqa1994@gmail.com") and ensures custom claims
 * and `/admins/{uid}` are synchronized.
 */
async function verifyAndEnsureSoleAdminUid(uid) {
  if (!uid || typeof uid !== "string") return false;
  try {
    const userRecord = await auth.getUser(uid);
    const email = normalizeEmail(userRecord.email);
    const isSoleAdmin =
      email === SOLE_AUTHORIZED_ADMIN_EMAIL && userRecord.disabled !== true;

    const currentClaims = userRecord.customClaims || {};
    if (isSoleAdmin) {
      if (currentClaims.admin !== true || currentClaims.role !== "sole_admin") {
        await auth.setCustomUserClaims(uid, {
          ...currentClaims,
          admin: true,
          role: "sole_admin",
          adminEmail: SOLE_AUTHORIZED_ADMIN_EMAIL,
        });
      }
      await db.collection("admins").doc(uid).set(
        {
          uid,
          email: SOLE_AUTHORIZED_ADMIN_EMAIL,
          enabled: true,
          role: "sole_admin",
          updatedAt: FieldValue.serverTimestamp(),
        },
        { merge: true }
      );
      return true;
    } else {
      if (currentClaims.admin === true) {
        const cleanedClaims = { ...currentClaims };
        delete cleanedClaims.admin;
        delete cleanedClaims.role;
        delete cleanedClaims.adminEmail;
        await auth.setCustomUserClaims(uid, cleanedClaims);
      }
      return false;
    }
  } catch (err) {
    console.error("verifyAndEnsureSoleAdminUid error:", err);
    return false;
  }
}

/**
 * Callable endpoint for the Android client to verify and synchronize Firebase
 * Custom Claims (`admin: true`) for the currently authenticated user.
 * Rejects any account other than "beqa1994@gmail.com".
 */
exports.syncSoleAdminCustomClaims = onCall(async (request) => {
  if (!request.auth || !request.auth.uid) {
    throw new HttpsError("unauthenticated", "Authentication is required.");
  }

  const callerUid = request.auth.uid;
  const isSoleAdmin = await verifyAndEnsureSoleAdminUid(callerUid);

  if (!isSoleAdmin) {
    throw new HttpsError(
      "permission-denied",
      "Only the sole authorized administrator (beqa1994@gmail.com) may obtain admin claims."
    );
  }

  return {
    authorized: true,
    uid: callerUid,
    email: SOLE_AUTHORIZED_ADMIN_EMAIL,
    customClaimAdmin: true,
  };
});

/**
 * Queries `/admin_fcm_tokens` for active tokens belonging to "beqa1994@gmail.com",
 * verifies each token's owner UID via Firebase Auth, sends the sanitized high-priority
 * FCM push notification exclusively to those verified admin device tokens, and cleans up
 * any invalid or expired tokens.
 */
async function dispatchSanitizedAdminOrderPush({
  notificationId,
  orderId,
  orderType,
  title,
  body,
  deepLink,
}) {
  const safeTitle = sanitizeNotificationText(title, "ახალი შეკვეთა");
  const safeBody = sanitizeNotificationText(
    body,
    "შემოვიდა ახალი შეკვეთა."
  );

  // Persist / ensure the canonical admin notification record in `/admin_notifications`
  const notifRef = db.collection("admin_notifications").doc(notificationId);
  await notifRef.set(
    {
      id: notificationId,
      notificationId,
      orderId,
      orderType,
      title: safeTitle,
      body: safeBody,
      deepLink,
      targetAdminEmail: SOLE_AUTHORIZED_ADMIN_EMAIL,
      targetRecipientEmail: SOLE_AUTHORIZED_ADMIN_EMAIL,
      read: false,
      createdAtMillis: Date.now(),
      createdAt: FieldValue.serverTimestamp(),
      updatedAt: FieldValue.serverTimestamp(),
    },
    { merge: true }
  );

  // Fetch active admin device tokens registered for "beqa1994@gmail.com"
  const tokenSnap = await db
    .collection("admin_fcm_tokens")
    .where("email", "==", SOLE_AUTHORIZED_ADMIN_EMAIL)
    .where("active", "==", true)
    .get();

  if (tokenSnap.empty) {
    return { sentCount: 0, cleanedCount: 0 };
  }

  // Verify UIDs and collect valid tokens across all authenticated admin devices
  const verifiedAdminUidCache = new Map();
  const validTokenDocs = [];
  const invalidDocRefs = [];

  for (const docSnap of tokenSnap.docs) {
    const data = docSnap.data() || {};
    const token = typeof data.token === "string" ? data.token.trim() : "";
    const userId =
      typeof data.userId === "string" && data.userId.trim()
        ? data.userId.trim()
        : typeof data.uid === "string"
        ? data.uid.trim()
        : "";
    const email = normalizeEmail(data.email);

    if (
      !isRealFcmRegistrationToken(token) ||
      !userId ||
      email !== SOLE_AUTHORIZED_ADMIN_EMAIL
    ) {
      invalidDocRefs.push(docSnap.ref);
      continue;
    }

    if (!verifiedAdminUidCache.has(userId)) {
      const ok = await verifyAndEnsureSoleAdminUid(userId);
      verifiedAdminUidCache.set(userId, ok);
    }

    if (verifiedAdminUidCache.get(userId) === true) {
      validTokenDocs.push({
        ref: docSnap.ref,
        token,
        deviceId: data.deviceId || "",
      });
    } else {
      invalidDocRefs.push(docSnap.ref);
    }
  }

  // Deduplicate tokens while keeping references for cleanup if expired
  const uniqueTokensMap = new Map();
  for (const item of validTokenDocs) {
    if (!uniqueTokensMap.has(item.token)) {
      uniqueTokensMap.set(item.token, []);
    }
    uniqueTokensMap.get(item.token).push(item.ref);
  }

  const tokens = Array.from(uniqueTokensMap.keys());
  let sentCount = 0;

  if (tokens.length > 0) {
    const canonicalClientTag = `notif_order_${orderType}_${orderId}`;
    // Strictly sanitized combined notification + data payload:
    // - `notification` ensures reliable OS system-tray display when the app is in background or closed.
    // - `data` carries deep-link metadata for Both onMessageReceived (foreground) and launch Intent extras (background/closed tap).
    const messagePayload = {
      tokens,
      notification: {
        title: safeTitle,
        body: safeBody,
      },
      data: {
        type: "admin_order",
        notificationId: String(canonicalClientTag),
        adminNotificationDocId: String(notificationId),
        orderId: String(orderId),
        orderType: String(orderType),
        title: safeTitle,
        body: safeBody,
        deepLink: String(deepLink),
        targetAdminEmail: SOLE_AUTHORIZED_ADMIN_EMAIL,
      },
      android: {
        priority: "high",
        ttl: DEFAULT_TTL_MS,
        collapseKey: String(canonicalClientTag),
        notification: {
          channelId: ADMIN_ORDERS_CHANNEL_ID,
          clickAction: "OPEN_ADMIN_ORDER_DEEP_LINK",
          tag: String(canonicalClientTag),
          sound: "default",
          defaultVibrateTimings: true,
          visibility: "PRIVATE",
        },
      },
    };

    const batchResponse = await messaging.sendEachForMulticast(messagePayload);
    sentCount = batchResponse.successCount;

    batchResponse.responses.forEach((resp, idx) => {
      if (!resp.success && resp.error) {
        const code = resp.error.code || "";
        if (
          code === "messaging/invalid-registration-token" ||
          code === "messaging/registration-token-not-registered" ||
          code === "messaging/invalid-argument"
        ) {
          const failedToken = tokens[idx];
          const refs = uniqueTokensMap.get(failedToken) || [];
          refs.forEach((r) => invalidDocRefs.push(r));
        }
      }
    });
  }

  // Delete invalid or unauthorized token documents
  if (invalidDocRefs.length > 0) {
    const batch = db.batch();
    invalidDocRefs.forEach((ref) => batch.delete(ref));
    await batch.commit();
  }

  return { sentCount, cleanedCount: invalidDocRefs.length };
}

/**
 * Sends a sanitized combined notification + data push to all active devices registered
 * for a specific customer `userId` in `/fcm_tokens`, cleaning up any expired/invalid tokens.
 */
async function dispatchCustomerUserPush({
  userId,
  notificationId,
  title,
  body,
  type = "service_update",
  targetTab = 4,
  targetProductId = 0,
}) {
  if (!userId || typeof userId !== "string") {
    return { sentCount: 0, cleanedCount: 0 };
  }

  const safeTitle = sanitizeNotificationText(title, "BEKASERVICE განახლება");
  const safeBody = sanitizeNotificationText(
    body,
    "თქვენი მოთხოვნის სტატუსი განახლდა."
  );

  const tokenSnap = await db
    .collection("fcm_tokens")
    .where("userId", "==", userId)
    .get();

  if (tokenSnap.empty) {
    return { sentCount: 0, cleanedCount: 0 };
  }

  const uniqueTokensMap = new Map();
  const invalidDocRefs = [];

  for (const docSnap of tokenSnap.docs) {
    const data = docSnap.data() || {};
    if (data.notificationsEnabled === false) {
      continue;
    }
    const token = typeof data.token === "string" ? data.token.trim() : "";
    if (!isRealFcmRegistrationToken(token)) {
      invalidDocRefs.push(docSnap.ref);
      continue;
    }
    if (!uniqueTokensMap.has(token)) {
      uniqueTokensMap.set(token, []);
    }
    uniqueTokensMap.get(token).push(docSnap.ref);
  }

  const tokens = Array.from(uniqueTokensMap.keys());
  let sentCount = 0;

  if (tokens.length > 0) {
    const messagePayload = {
      tokens,
      notification: {
        title: safeTitle,
        body: safeBody,
      },
      data: {
        notificationId: String(notificationId),
        title: safeTitle,
        body: safeBody,
        type: String(type),
        targetTab: String(targetTab),
        targetProductId: String(targetProductId),
      },
      android: {
        priority: "high",
        ttl: DEFAULT_TTL_MS,
        collapseKey: String(notificationId),
        notification: {
          channelId: GENERAL_PUSH_CHANNEL_ID,
          tag: String(notificationId),
          sound: "default",
          defaultVibrateTimings: true,
          visibility: "PRIVATE",
        },
      },
    };

    const batchResponse = await messaging.sendEachForMulticast(messagePayload);
    sentCount = batchResponse.successCount;

    batchResponse.responses.forEach((resp, idx) => {
      if (!resp.success && resp.error) {
        const code = resp.error.code || "";
        if (
          code === "messaging/invalid-registration-token" ||
          code === "messaging/registration-token-not-registered" ||
          code === "messaging/invalid-argument"
        ) {
          const failedToken = tokens[idx];
          const refs = uniqueTokensMap.get(failedToken) || [];
          refs.forEach((r) => invalidDocRefs.push(r));
        }
      }
    });
  }

  if (invalidDocRefs.length > 0) {
    const batch = db.batch();
    invalidDocRefs.forEach((ref) => batch.delete(ref));
    await batch.commit();
  }

  return { sentCount, cleanedCount: invalidDocRefs.length };
}

/**
 * Firestore Trigger (Named Database): When a new IMEI Check request is created in
 * `imei_requests/{requestId}`, send a sanitized push notification exclusively to
 * the sole authorized administrator's devices.
 */
exports.onImeiRequestCreated = onDocumentCreated(
  {
    document: "imei_requests/{requestId}",
    database: FIRESTORE_DATABASE_ID,
  },
  async (event) => {
    const requestId = event.params.requestId;
    if (!requestId) return;

    const notificationId = `admin_order_${requestId}`;
    const deepLink = `bekaservice://admin/orders/imei/${requestId}`;

    await dispatchSanitizedAdminOrderPush({
      notificationId,
      orderId: requestId,
      orderType: "imei_check",
      title: "ახალი IMEI შეკვეთა",
      body: "შემოვიდა ახალი IMEI შემოწმების მოთხოვნა.",
      deepLink,
    });
  }
);

/**
 * Firestore Trigger (Named Database): When a new service/product order is created in
 * `orders/{orderId}`, send a sanitized push notification exclusively to the sole
 * authorized administrator's devices.
 */
exports.onServiceOrderCreated = onDocumentCreated(
  {
    document: "orders/{orderId}",
    database: FIRESTORE_DATABASE_ID,
  },
  async (event) => {
    const orderId = event.params.orderId;
    if (!orderId) return;

    const notificationId = `admin_order_${orderId}`;
    const deepLink = `bekaservice://admin/orders/service/${orderId}`;

    await dispatchSanitizedAdminOrderPush({
      notificationId,
      orderId,
      orderType: "service_order",
      title: "ახალი შეკვეთა",
      body: "შემოვიდა ახალი სერვისის შეკვეთა.",
      deepLink,
    });
  }
);

/**
 * Firestore Trigger (Named Database): When a broadcast or targeted notification is created
 * in `notifications/{notificationId}` by the administrator, dispatch a high-priority
 * combined notification + data FCM push so devices receive it in background and closed states.
 */
exports.onBroadcastNotificationCreated = onDocumentCreated(
  {
    document: "notifications/{notificationId}",
    database: FIRESTORE_DATABASE_ID,
  },
  async (event) => {
    const notificationId = event.params.notificationId;
    const snap = event.data;
    if (!notificationId || !snap) return;

    const data = snap.data() || {};
    const senderEmail = normalizeEmail(data.senderEmail);
    if (senderEmail && senderEmail !== SOLE_AUTHORIZED_ADMIN_EMAIL) {
      return;
    }

    const safeTitle = sanitizeNotificationText(
      data.title,
      "BEKASERVICE შეტყობინება"
    );
    const safeBody = sanitizeNotificationText(
      data.body,
      "ახალი შეტყობინება BEKASERVICE-ისგან."
    );
    const type = typeof data.type === "string" ? data.type : "announcement";
    const targetProductId =
      typeof data.targetProductId === "number" ? data.targetProductId : 0;
    const targetTab = typeof data.targetTab === "number" ? data.targetTab : -1;
    const audience = typeof data.audience === "string" ? data.audience : "all_users";

    // If targeted to a specific user (`user:<uid>`), send only to that user's registered devices
    if (audience.startsWith("user:")) {
      const targetUserId = audience.slice("user:".length).trim();
      if (targetUserId) {
        await dispatchCustomerUserPush({
          userId: targetUserId,
          notificationId,
          title: safeTitle,
          body: safeBody,
          type,
          targetTab: targetTab >= 0 ? targetTab : 2,
          targetProductId,
        });
      }
      return;
    }

    // Otherwise broadcast to the `bekaservice_announcements` topic with high priority
    const topicMessage = {
      topic: BROADCAST_TOPIC,
      notification: {
        title: safeTitle,
        body: safeBody,
      },
      data: {
        notificationId: String(notificationId),
        title: safeTitle,
        body: safeBody,
        type: String(type),
        targetProductId: String(targetProductId),
        targetTab: String(targetTab),
      },
      android: {
        priority: "high",
        ttl: DEFAULT_TTL_MS,
        collapseKey: String(notificationId),
        notification: {
          channelId: GENERAL_PUSH_CHANNEL_ID,
          tag: String(notificationId),
          sound: "default",
          defaultVibrateTimings: true,
          visibility: "PUBLIC",
        },
      },
    };

    await messaging.send(topicMessage);
  }
);

/**
 * Firestore Trigger (Named Database): When an IMEI report is published or updated in
 * `imei_reports/{requestId}`, notify ONLY the customer who owns that IMEI check request
 * (using sanitized status text, never full IMEI or raw report content).
 */
exports.onImeiReportWritten = onDocumentWritten(
  {
    document: "imei_reports/{requestId}",
    database: FIRESTORE_DATABASE_ID,
  },
  async (event) => {
    const requestId = event.params.requestId;
    const afterSnap = event.data ? event.data.after : null;
    if (!requestId || !afterSnap || !afterSnap.exists) return;

    const beforeData =
      event.data && event.data.before && event.data.before.exists
        ? event.data.before.data() || {}
        : null;
    const afterData = afterSnap.data() || {};

    if (afterData.isPublished !== true) return;
    if (
      beforeData &&
      beforeData.isPublished === true &&
      beforeData.status === afterData.status
    ) {
      return;
    }

    const userId = typeof afterData.userId === "string" ? afterData.userId.trim() : "";
    if (!userId) return;

    const brandName =
      typeof afterData.brandName === "string" && afterData.brandName.trim()
        ? afterData.brandName.trim()
        : "მოწყობილობის";
    const status = typeof afterData.status === "string" ? afterData.status : "completed";

    let title = "IMEI შემოწმების პასუხი მზადაა";
    let body = `${brandName} IMEI შემოწმების შედეგი გამოქვეყნდა. გახსენით აპლიკაცია დეტალების სანახავად.`;
    if (status === "waiting_for_information") {
      title = "IMEI შემოწმება: საჭიროა დაზუსტება";
      body = `${brandName} IMEI მოთხოვნაზე საჭიროა დამატებითი ინფორმაცია.`;
    } else if (status === "rejected") {
      title = "IMEI შემოწმების სტატუსი განახლდა";
      body = `${brandName} IMEI მოთხოვნის სტატუსი განახლდა.`;
    }

    await dispatchCustomerUserPush({
      userId,
      notificationId: `notif_imei_status_${requestId}_${status}`,
      title,
      body,
      type: "imei_update",
      targetTab: 2,
      targetProductId: 0,
    });
  }
);

/**
 * Firestore Trigger (Named Database): When a customer's order status changes in `orders/{orderId}`,
 * send a sanitized status update push exclusively to that customer's registered devices.
 */
exports.onServiceOrderStatusUpdated = onDocumentUpdated(
  {
    document: "orders/{orderId}",
    database: FIRESTORE_DATABASE_ID,
  },
  async (event) => {
    const orderId = event.params.orderId;
    if (!orderId || !event.data) return;

    const beforeData = event.data.before.data() || {};
    const afterData = event.data.after.data() || {};

    if (!afterData.status || beforeData.status === afterData.status) {
      return;
    }

    const userId = typeof afterData.userId === "string" ? afterData.userId.trim() : "";
    if (!userId) return;

    const statusText = String(afterData.status).slice(0, 60);
    await dispatchCustomerUserPush({
      userId,
      notificationId: `notif_order_status_${orderId}_${statusText.hashCode ? statusText.hashCode() : Date.now()}`,
      title: "შეკვეთის სტატუსი განახლდა",
      body: `თქვენი შეკვეთის ახალი სტატუსია: ${statusText}`,
      type: "service_update",
      targetTab: 4,
      targetProductId: 0,
    });
  }
);
