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
  onDocumentUpdated,
} = require("firebase-functions/v2/firestore");
const { onCall, HttpsError } = require("firebase-functions/v2/https");
const admin = require("firebase-admin");
const { getFirestore, FieldValue } = require("firebase-admin/firestore");
const crypto = require("node:crypto");

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

const NOTIFICATION_VISIBILITY = Object.freeze({
  PRIVATE_USER: "PRIVATE_USER",
  ADMIN_ONLY: "ADMIN_ONLY",
  PUBLIC: "PUBLIC",
});

const PUBLIC_SAFE_NOTIFICATION_TYPES = new Set([
  "announcement",
  "product",
  "product_offer",
  "promotion",
  "system_info",
  "maintenance",
  "product_restock",
  "service_update",
  "system_update",
  "app_update",
]);

/**
 * Normalizes an email address for strict comparison.
 */
function normalizeEmail(email) {
  return typeof email === "string" ? email.trim().toLowerCase() : "";
}

function isAnonymousCaller(request) {
  return Boolean(
    request &&
      request.auth &&
      request.auth.token &&
      request.auth.token.firebase &&
      request.auth.token.firebase.sign_in_provider === "anonymous"
  );
}

function requireRegisteredAuth(request) {
  if (!request || !request.auth || !request.auth.uid || !request.auth.token) {
    throw new HttpsError("unauthenticated", "Authentication is required.");
  }
  if (isAnonymousCaller(request)) {
    throw new HttpsError(
      "permission-denied",
      "Anonymous sessions are not permitted for registered-user resources."
    );
  }
  return request.auth.uid;
}

function computeTokenHash(token) {
  return crypto.createHash("sha256").update(String(token || "")).digest("hex");
}

function sanitizeDocSegment(raw, maxLen = 64) {
  return String(raw || "")
    .trim()
    .replace(/[^a-zA-Z0-9_-]/g, "_")
    .slice(0, maxLen);
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
 * sole authorized administrator ("beqa1994@gmail.com") with a verified email,
 * and ensures custom claims (`admin: true`) and `/admins/{uid}` are synchronized
 * while preserving any unrelated existing custom claims.
 */
async function verifyAndEnsureSoleAdminUid(uid, tokenClaims = null) {
  if (!uid || typeof uid !== "string") return false;
  try {
    const userRecord = await auth.getUser(uid);
    const email = normalizeEmail(userRecord.email);
    const tokenEmail =
      tokenClaims && typeof tokenClaims.email === "string"
        ? normalizeEmail(tokenClaims.email)
        : email;
    const isEmailVerified =
      userRecord.emailVerified === true ||
      Boolean(
        tokenClaims &&
          (tokenClaims.email_verified === true ||
            (tokenClaims.firebase &&
              tokenClaims.firebase.sign_in_provider === "password"))
      );
    const isSoleAdmin =
      email === SOLE_AUTHORIZED_ADMIN_EMAIL &&
      tokenEmail === SOLE_AUTHORIZED_ADMIN_EMAIL &&
      userRecord.disabled !== true &&
      isEmailVerified;

    const currentClaims = userRecord.customClaims || {};
    if (isSoleAdmin) {
      if (
        currentClaims.admin !== true ||
        currentClaims.role !== "sole_admin" ||
        currentClaims.adminEmail !== SOLE_AUTHORIZED_ADMIN_EMAIL
      ) {
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
          customClaimAdmin: true,
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
 * - Verifies caller via Firebase Authentication context (`request.auth`).
 * - Never accepts an arbitrary UID or email from `request.data` to grant admin access.
 * - Requires exact email "beqa1994@gmail.com" and verified email status.
 */
exports.syncSoleAdminCustomClaims = onCall(async (request) => {
  const callerUid = requireRegisteredAuth(request);
  const clientData =
    request.data && typeof request.data === "object" ? request.data : {};

  if (
    typeof clientData.uid === "string" &&
    clientData.uid.trim() !== "" &&
    clientData.uid.trim() !== callerUid
  ) {
    throw new HttpsError(
      "permission-denied",
      "Client-supplied UID override is forbidden."
    );
  }

  if (
    typeof clientData.email === "string" &&
    clientData.email.trim() !== "" &&
    normalizeEmail(clientData.email) !== SOLE_AUTHORIZED_ADMIN_EMAIL
  ) {
    throw new HttpsError(
      "permission-denied",
      "Client-supplied email override is forbidden."
    );
  }

  const callerTokenEmail = normalizeEmail(request.auth.token.email);
  const callerEmailVerified = request.auth.token.email_verified !== false;
  if (callerTokenEmail !== SOLE_AUTHORIZED_ADMIN_EMAIL || !callerEmailVerified) {
    await verifyAndEnsureSoleAdminUid(callerUid, request.auth.token);
    throw new HttpsError(
      "permission-denied",
      "Only the sole authorized administrator (beqa1994@gmail.com) with a verified email may obtain admin claims."
    );
  }

  const isSoleAdmin = await verifyAndEnsureSoleAdminUid(
    callerUid,
    request.auth.token
  );

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
 * Trusted Backend Endpoint: Atomically claims an FCM token + deviceId for the currently
 * authenticated registered user, and deactivates any previous UID's association with the
 * same FCM token or physical deviceId. Preserves other devices belonging to the same user.
 */
exports.claimFcmToken = onCall(async (request) => {
  const callerUid = requireRegisteredAuth(request);
  const data = request.data && typeof request.data === "object" ? request.data : {};
  const cleanToken = typeof data.token === "string" ? data.token.trim() : "";
  if (!isRealFcmRegistrationToken(cleanToken)) {
    throw new HttpsError("invalid-argument", "Valid FCM token is required.");
  }
  const tokenHash = computeTokenHash(cleanToken);
  const cleanDeviceId = sanitizeDocSegment(data.deviceId, 64) || `dev_${tokenHash.slice(0, 12)}`;
  const deviceModel = typeof data.deviceModel === "string" ? data.deviceModel.trim().slice(0, 120) : "Android";
  const notificationsEnabled = data.notificationsEnabled !== false;
  const callerEmail = normalizeEmail(request.auth.token && request.auth.token.email);

  const batch = db.batch();
  const targetDocId = `${callerUid}_${cleanDeviceId}`.slice(0, 120);
  const targetDocRef = db.collection("fcm_tokens").doc(targetDocId);

  // Deactivate/remove any OTHER user's token docs matching this physical token or deviceId
  const sameTokenSnap = await db.collection("fcm_tokens").where("token", "==", cleanToken).get();
  for (const snap of sameTokenSnap.docs) {
    const docData = snap.data() || {};
    if (docData.userId && docData.userId !== callerUid) {
      batch.set(
        snap.ref,
        {
          active: false,
          notificationsEnabled: false,
          updatedAt: FieldValue.serverTimestamp(),
        },
        { merge: true }
      );
    } else if (docData.userId === callerUid && snap.id !== targetDocId && snap.id !== callerUid && docData.deviceId === cleanDeviceId) {
      batch.delete(snap.ref);
    }
  }

  if (cleanDeviceId) {
    const sameDeviceSnap = await db.collection("fcm_tokens").where("deviceId", "==", cleanDeviceId).get();
    for (const snap of sameDeviceSnap.docs) {
      const docData = snap.data() || {};
      if (docData.userId && docData.userId !== callerUid) {
        batch.set(
          snap.ref,
          {
            active: false,
            notificationsEnabled: false,
            updatedAt: FieldValue.serverTimestamp(),
          },
          { merge: true }
        );
      }
    }
  }

  const tokenPayload = {
    userId: callerUid,
    token: cleanToken.slice(0, 4096),
    tokenHash,
    deviceId: cleanDeviceId,
    email: callerEmail.slice(0, 150),
    deviceModel,
    platform: "android",
    notificationsEnabled,
    active: true,
    updatedAt: FieldValue.serverTimestamp(),
  };

  batch.set(targetDocRef, tokenPayload, { merge: true });
  batch.set(db.collection("fcm_tokens").doc(callerUid), tokenPayload, { merge: true });

  // Handle admin token isolation: only verified sole admin may hold active /admin_fcm_tokens
  const isVerifiedAdmin = await verifyAndEnsureSoleAdminUid(callerUid, request.auth.token);
  const adminSameTokenSnap = await db.collection("admin_fcm_tokens").where("token", "==", cleanToken).get();
  for (const snap of adminSameTokenSnap.docs) {
    const d = snap.data() || {};
    const ownerUid = d.userId || d.uid || "";
    if (!isVerifiedAdmin || ownerUid !== callerUid) {
      batch.delete(snap.ref);
    }
  }
  const adminSameDeviceSnap = await db.collection("admin_fcm_tokens").where("deviceId", "==", cleanDeviceId).get();
  for (const snap of adminSameDeviceSnap.docs) {
    const d = snap.data() || {};
    const ownerUid = d.userId || d.uid || "";
    if (!isVerifiedAdmin || ownerUid !== callerUid) {
      batch.delete(snap.ref);
    }
  }

  if (isVerifiedAdmin) {
    const adminTokenId = `adm_tok_${cleanDeviceId}_${tokenHash.slice(0, 10)}`;
    batch.set(
      db.collection("admin_fcm_tokens").doc(adminTokenId),
      {
        id: adminTokenId,
        tokenId: adminTokenId,
        token: cleanToken.slice(0, 4096),
        userId: callerUid,
        email: SOLE_AUTHORIZED_ADMIN_EMAIL,
        deviceId: cleanDeviceId,
        deviceModel,
        platform: "android",
        active: true,
        updatedAt: FieldValue.serverTimestamp(),
      },
      { merge: true }
    );
  }

  await batch.commit();
  return {
    success: true,
    uid: callerUid,
    deviceId: cleanDeviceId,
    active: true,
    isAdmin: isVerifiedAdmin,
  };
});

/**
 * Trusted Backend Endpoint: Releases/deactivates ONLY the caller's current device FCM token on logout.
 * Preserves the caller's tokens on other physical devices so multi-device accounts remain signed in elsewhere.
 */
exports.releaseFcmToken = onCall(async (request) => {
  const callerUid = requireRegisteredAuth(request);
  const data = request.data && typeof request.data === "object" ? request.data : {};
  const cleanToken = typeof data.token === "string" ? data.token.trim() : "";
  const cleanDeviceId = sanitizeDocSegment(data.deviceId, 64);

  const batch = db.batch();
  if (cleanDeviceId) {
    batch.delete(db.collection("fcm_tokens").doc(`${callerUid}_${cleanDeviceId}`.slice(0, 120)));
  }

  const legacyRef = db.collection("fcm_tokens").doc(callerUid);
  const legacySnap = await legacyRef.get();
  if (legacySnap.exists) {
    const legacyData = legacySnap.data() || {};
    const matchesCurrentDevice =
      (cleanDeviceId && legacyData.deviceId === cleanDeviceId) ||
      (cleanToken && legacyData.token === cleanToken) ||
      (!cleanDeviceId && !cleanToken);
    if (matchesCurrentDevice) {
      batch.delete(legacyRef);
    }
  }

  if (cleanToken) {
    const ownedTokenDocs = await db
      .collection("fcm_tokens")
      .where("userId", "==", callerUid)
      .where("token", "==", cleanToken)
      .get();
    ownedTokenDocs.forEach((snap) => batch.delete(snap.ref));

    const ownedAdminTokenDocs = await db
      .collection("admin_fcm_tokens")
      .where("token", "==", cleanToken)
      .get();
    ownedAdminTokenDocs.forEach((snap) => {
      const d = snap.data() || {};
      if ((d.userId || d.uid || "") === callerUid) {
        batch.delete(snap.ref);
      }
    });
  }

  if (cleanDeviceId) {
    const ownedAdminDeviceDocs = await db
      .collection("admin_fcm_tokens")
      .where("deviceId", "==", cleanDeviceId)
      .get();
    ownedAdminDeviceDocs.forEach((snap) => {
      const d = snap.data() || {};
      if ((d.userId || d.uid || "") === callerUid) {
        batch.delete(snap.ref);
      }
    });
  }

  await batch.commit();
  return { success: true, releasedUid: callerUid, deviceId: cleanDeviceId };
});

/**
 * Trusted Backend Endpoint: Called on fresh launch / unauthenticated or anonymous session
 * when a device already holds an FCM registration token, proving possession of the high-entropy
 * token so any stale pre-uninstall/logged-out association on that exact token is deactivated.
 */
exports.unbindUnauthenticatedDeviceToken = onCall(async (request) => {
  const data = request.data && typeof request.data === "object" ? request.data : {};
  const cleanToken = typeof data.token === "string" ? data.token.trim() : "";
  if (!isRealFcmRegistrationToken(cleanToken) || cleanToken.length < 80) {
    throw new HttpsError("invalid-argument", "High-entropy FCM token required.");
  }

  // Only deactivate if caller is NOT logged in as a registered owner of those docs
  const callerUid =
    request.auth && request.auth.uid && !isAnonymousCaller(request)
      ? request.auth.uid
      : "";

  const batch = db.batch();
  let deactivatedCount = 0;

  const matchingUserTokens = await db
    .collection("fcm_tokens")
    .where("token", "==", cleanToken)
    .get();
  matchingUserTokens.forEach((snap) => {
    const d = snap.data() || {};
    if (!callerUid || d.userId !== callerUid) {
      batch.set(
        snap.ref,
        {
          active: false,
          notificationsEnabled: false,
          updatedAt: FieldValue.serverTimestamp(),
        },
        { merge: true }
      );
      deactivatedCount++;
    }
  });

  const matchingAdminTokens = await db
    .collection("admin_fcm_tokens")
    .where("token", "==", cleanToken)
    .get();
  matchingAdminTokens.forEach((snap) => {
    const d = snap.data() || {};
    const ownerUid = d.userId || d.uid || "";
    if (!callerUid || ownerUid !== callerUid) {
      batch.delete(snap.ref);
      deactivatedCount++;
    }
  });

  if (deactivatedCount > 0) {
    await batch.commit();
  }
  return { success: true, deactivatedCount };
});

/**
 * Admin-only callable to classify legacy `/notifications` records missing `visibility`
 * without ever promoting ambiguous or customer-specific records to `PUBLIC`.
 */
exports.migrateLegacyNotificationClassifications = onCall(async (request) => {
  const callerUid = requireRegisteredAuth(request);
  const isSoleAdmin = await verifyAndEnsureSoleAdminUid(callerUid, request.auth.token);
  if (!isSoleAdmin) {
    throw new HttpsError("permission-denied", "Admin access required.");
  }

  const snap = await db.collection("notifications").limit(500).get();
  const batch = db.batch();
  let migratedCount = 0;

  snap.forEach((docSnap) => {
    const d = docSnap.data() || {};
    if (d.visibility) return;

    const audience = typeof d.audience === "string" ? d.audience.trim() : "";
    const orderId = typeof d.orderId === "string" ? d.orderId.trim() : "";
    const orderNumber = typeof d.orderNumber === "string" ? d.orderNumber.trim() : "";
    const notifType = typeof d.type === "string" ? d.type.trim() : "announcement";

    if (audience.startsWith("user:") && audience.slice(5).trim()) {
      const recipientUid = audience.slice(5).trim();
      batch.set(
        docSnap.ref,
        {
          visibility: NOTIFICATION_VISIBILITY.PRIVATE_USER,
          recipientUid,
          relatedEntityId: orderId || "",
          isRead: Boolean(d.isRead),
          updatedAt: FieldValue.serverTimestamp(),
        },
        { merge: true }
      );
      migratedCount++;
    } else if (
      (audience === "all" || audience === "all_users") &&
      !orderId &&
      !orderNumber &&
      PUBLIC_SAFE_NOTIFICATION_TYPES.has(notifType)
    ) {
      batch.set(
        docSnap.ref,
        {
          visibility: NOTIFICATION_VISIBILITY.PUBLIC,
          recipientUid: "",
          updatedAt: FieldValue.serverTimestamp(),
        },
        { merge: true }
      );
      migratedCount++;
    } else {
      // Never promote ambiguous notifications to PUBLIC!
      batch.set(
        docSnap.ref,
        {
          visibility: NOTIFICATION_VISIBILITY.ADMIN_ONLY,
          audience: "admin_only",
          recipientUid: "",
          updatedAt: FieldValue.serverTimestamp(),
        },
        { merge: true }
      );
      migratedCount++;
    }
  });

  if (migratedCount > 0) {
    await batch.commit();
  }
  return { success: true, migratedCount };
});

/**
 * Queries `/admin_fcm_tokens` for active tokens belonging to "beqa1994@gmail.com",
 * verifies each token's owner UID via Firebase Auth, sends a data-only high-priority
 * FCM push notification exclusively to those verified admin device tokens (so OS never
 * auto-displays stale notification blocks if device switched/logged out), and cleans up
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
      visibility: NOTIFICATION_VISIBILITY.ADMIN_ONLY,
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
    // Data-only high-priority message for ADMIN_ONLY notifications:
    // Ensures BekaFirebaseMessagingService enforces active admin authorization before OS display.
    const messagePayload = {
      tokens,
      data: {
        visibility: NOTIFICATION_VISIBILITY.ADMIN_ONLY,
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
 * Sends a sanitized data-only PRIVATE_USER push strictly to verified, active devices
 * registered for `userId` in `/fcm_tokens`:
 * - Verifies `userId` belongs to a real, non-disabled, non-anonymous Firebase Auth user.
 * - Rejects inactive (`active === false`), opted-out, or stale tokens claimed by a newer account.
 * - Uses data-only payload so Android always runs `BekaFirebaseMessagingService` recipient verification
 *   and displays a neutral lockscreen version (`VISIBILITY_PRIVATE`).
 */
async function dispatchCustomerUserPush({
  userId,
  notificationId,
  title,
  body,
  type = "order_update",
  targetTab = 4,
  targetProductId = 0,
  targetProductDocId = "",
  orderId = "",
  orderNumber = "",
}) {
  if (!userId || typeof userId !== "string" || !userId.trim()) {
    return { sentCount: 0, cleanedCount: 0 };
  }
  const cleanRecipientUid = userId.trim();

  // Verify recipient account exists in Firebase Auth, is enabled, and is NOT anonymous
  try {
    const recipientRecord = await auth.getUser(cleanRecipientUid);
    if (
      !recipientRecord ||
      recipientRecord.disabled === true ||
      !Array.isArray(recipientRecord.providerData) ||
      recipientRecord.providerData.length === 0
    ) {
      return { sentCount: 0, cleanedCount: 0 };
    }
  } catch (err) {
    console.warn("dispatchCustomerUserPush rejected unverified recipient:", err.message);
    return { sentCount: 0, cleanedCount: 0 };
  }

  const safeTitle = sanitizeNotificationText(title, "BSTORE განახლება");
  const safeBody = sanitizeNotificationText(
    body,
    "თქვენი მოთხოვნის სტატუსი განახლდა."
  );

  const tokenSnap = await db
    .collection("fcm_tokens")
    .where("userId", "==", cleanRecipientUid)
    .get();

  if (tokenSnap.empty) {
    return { sentCount: 0, cleanedCount: 0 };
  }

  const uniqueTokensMap = new Map();
  const invalidDocRefs = [];

  for (const docSnap of tokenSnap.docs) {
    const data = docSnap.data() || {};
    if (data.active === false || data.notificationsEnabled === false) {
      continue;
    }
    const token = typeof data.token === "string" ? data.token.trim() : "";
    if (!isRealFcmRegistrationToken(token)) {
      invalidDocRefs.push(docSnap.ref);
      continue;
    }

    // Cross-check if the same physical FCM token is actively claimed more recently by another user
    const currentOwnersSnap = await db
      .collection("fcm_tokens")
      .where("token", "==", token)
      .get();
    const docUpdatedMs = docSnap.updateTime ? docSnap.updateTime.toMillis() : 0;
    let stolenOrStale = false;
    for (const ownerDoc of currentOwnersSnap.docs) {
      const ownerData = ownerDoc.data() || {};
      if (
        ownerData.userId &&
        ownerData.userId !== cleanRecipientUid &&
        ownerData.active !== false
      ) {
        const ownerUpdatedMs = ownerDoc.updateTime ? ownerDoc.updateTime.toMillis() : 0;
        if (ownerUpdatedMs >= docUpdatedMs) {
          stolenOrStale = true;
          break;
        }
      }
    }
    if (stolenOrStale) {
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
      data: {
        visibility: NOTIFICATION_VISIBILITY.PRIVATE_USER,
        recipientUid: cleanRecipientUid,
        audience: `user:${cleanRecipientUid}`,
        notificationId: String(notificationId),
        title: safeTitle,
        body: safeBody,
        type: String(type),
        targetTab: String(targetTab),
        targetProductId: String(targetProductId),
        targetProductDocId: String(targetProductDocId || ""),
        productDocId: String(targetProductDocId || ""),
        orderId: String(orderId || ""),
        orderNumber: String(orderNumber || ""),
        relatedEntityId: String(orderId || targetProductDocId || targetProductId || ""),
      },
      android: {
        priority: "high",
        ttl: DEFAULT_TTL_MS,
        collapseKey: String(notificationId),
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

    const orderData = event.data ? event.data.data() || {} : {};
    const orderNum =
      typeof orderData.orderNumber === "string" && orderData.orderNumber.trim()
        ? orderData.orderNumber.trim()
        : `#BS-${orderId.slice(-6).toUpperCase()}`;
    const isStockInquiry =
      orderId.startsWith("req_") ||
      orderNum.startsWith("#REQ-") ||
      (typeof orderData.serviceType === "string" &&
        orderData.serviceType.includes("პროდუქტის მოთხოვნა"));
    const summaryTitle =
      typeof orderData.deviceModel === "string" && orderData.deviceModel.trim()
        ? orderData.deviceModel.trim()
        : typeof orderData.serviceType === "string"
        ? orderData.serviceType.trim()
        : "";
    const priceText =
      typeof orderData.price === "string" && orderData.price.trim()
        ? orderData.price.trim()
        : typeof orderData.totalAmount === "number" && orderData.totalAmount > 0
        ? `${Math.round(orderData.totalAmount)} ₾`
        : "";

    const pushTitle = isStockInquiry
      ? `📦 პროდუქტის მოთხოვნა ${orderNum}`
      : `ახალი შეკვეთა ${orderNum}`;
    const bodyParts = [
      isStockInquiry ? `პოტენციური შეკვეთა ${orderNum}` : `ახალი შეკვეთა ${orderNum}`,
    ];
    if (summaryTitle) bodyParts.push(summaryTitle);
    if (!isStockInquiry && priceText) bodyParts.push(priceText);

    const notificationId = `admin_order_${orderId}`;
    const deepLink = `bekaservice://admin/orders/service/${orderId}`;

    await dispatchSanitizedAdminOrderPush({
      notificationId,
      orderId,
      orderType: "service_order",
      title: pushTitle,
      body: bodyParts.join(" • "),
      deepLink,
    });
  }
);

/**
 * Firestore Trigger (Named Database): Enforces strict PUBLIC vs PRIVATE_USER vs ADMIN_ONLY
 * classification when a notification document is created in `notifications/{notificationId}`.
 * NEVER broadcasts to `BROADCAST_TOPIC` unless `visibility === "PUBLIC"` AND all public invariants hold.
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
      "BSTORE შეტყობინება"
    );
    const safeBody = sanitizeNotificationText(
      data.body,
      "ახალი შეტყობინება BSTORE-ისგან."
    );
    const type = typeof data.type === "string" ? data.type.trim() : "announcement";
    const targetProductId =
      typeof data.targetProductId === "number" ? data.targetProductId : 0;
    const targetProductDocId =
      typeof data.targetProductDocId === "string" && data.targetProductDocId.trim()
        ? data.targetProductDocId.trim()
        : typeof data.productDocId === "string" && data.productDocId.trim()
        ? data.productDocId.trim()
        : targetProductId > 0
        ? `prod_${targetProductId}`
        : "";
    const targetTab = typeof data.targetTab === "number" ? data.targetTab : -1;
    const orderId = typeof data.orderId === "string" ? data.orderId.trim() : "";
    const orderNumber =
      typeof data.orderNumber === "string" ? data.orderNumber.trim() : "";
    const audience = typeof data.audience === "string" ? data.audience.trim() : "";
    const visibility = typeof data.visibility === "string" ? data.visibility.trim() : "";
    const explicitRecipientUid =
      typeof data.recipientUid === "string" ? data.recipientUid.trim() : "";

    const targetUserId =
      explicitRecipientUid ||
      (audience.startsWith("user:") ? audience.slice("user:".length).trim() : "");

    // Route PRIVATE_USER notifications strictly to verified user device tokens (NEVER to topic)
    if (
      visibility === NOTIFICATION_VISIBILITY.PRIVATE_USER ||
      audience.startsWith("user:") ||
      targetUserId !== "" ||
      orderId !== "" ||
      orderNumber !== ""
    ) {
      if (targetUserId) {
        await dispatchCustomerUserPush({
          userId: targetUserId,
          notificationId,
          title: safeTitle,
          body: safeBody,
          type,
          targetTab: targetTab >= 0 ? targetTab : 4,
          targetProductId,
          targetProductDocId,
          orderId,
          orderNumber,
        });
      }
      return;
    }

    if (visibility === NOTIFICATION_VISIBILITY.ADMIN_ONLY || audience === "admin_only") {
      return;
    }

    // Strict PUBLIC topic gate: ONLY explicit visibility === "PUBLIC" with public-safe type and audience
    if (
      visibility !== NOTIFICATION_VISIBILITY.PUBLIC ||
      (audience !== "all" && audience !== "all_users") ||
      !PUBLIC_SAFE_NOTIFICATION_TYPES.has(type)
    ) {
      console.warn(
        `onBroadcastNotificationCreated blocked ambiguous/unclassified topic broadcast for ${notificationId}`
      );
      return;
    }

    const topicMessage = {
      topic: BROADCAST_TOPIC,
      notification: {
        title: safeTitle,
        body: safeBody,
      },
      data: {
        visibility: NOTIFICATION_VISIBILITY.PUBLIC,
        audience: "all",
        notificationId: String(notificationId),
        title: safeTitle,
        body: safeBody,
        type: String(type),
        targetProductId: String(targetProductId),
        targetProductDocId: String(targetProductDocId),
        productDocId: String(targetProductDocId),
        productId: String(targetProductDocId || (targetProductId > 0 ? targetProductId : "")),
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
 * Firestore Trigger (Named Database): When a customer's order status or paymentStatus changes in `orders/{orderId}`,
 * send a sanitized PRIVATE_USER push exclusively to that customer's or sole admin's verified active devices.
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

    const userId = typeof afterData.userId === "string" ? afterData.userId.trim() : "";
    if (!userId) return;

    const orderNum =
      typeof afterData.orderNumber === "string" && afterData.orderNumber.trim()
        ? afterData.orderNumber.trim()
        : `#BS-${orderId.slice(-6).toUpperCase()}`;

    // 1. Notify sole admin when customer marks bank transfer as 'verification_pending'
    if (
      afterData.paymentStatus === "verification_pending" &&
      beforeData.paymentStatus !== "verification_pending"
    ) {
      await dispatchSanitizedAdminOrderPush({
        notificationId: `admin_transfer_${orderId}`,
        orderId,
        orderType: "service_order",
        title: `გადარიცხვა დადასტურდა ${orderNum}`,
        body: `მომხმარებელმა დაადასტურა ${orderNum} შეკვეთის თანხის გადმორიცხვა. საჭიროა საბანკო ანგარიშზე შემოწმება.`,
        deepLink: `bekaservice://admin/orders/service/${orderId}`,
      });
    }

    // 2. Notify customer when paymentStatus transitions to 'paid' (explicit PRIVATE_USER record + FCM)
    if (
      afterData.paymentStatus === "paid" &&
      beforeData.paymentStatus !== "paid"
    ) {
      const paidNotifId = `notif_order_paid_${orderId}`;
      const paidTitle = `გადახდა დადასტურდა ${orderNum}`;
      const paidBody = `თქვენი ${orderNum} შეკვეთის გადახდა დადასტურდა.`;
      try {
        await db.collection("notifications").doc(paidNotifId).create({
          id: paidNotifId,
          title: paidTitle,
          body: paidBody,
          type: "order_update",
          visibility: NOTIFICATION_VISIBILITY.PRIVATE_USER,
          recipientUid: userId,
          relatedEntityId: orderId,
          targetProductId: 0,
          targetTab: 4,
          orderId,
          orderNumber: orderNum,
          senderEmail: SOLE_AUTHORIZED_ADMIN_EMAIL,
          audience: `user:${userId}`,
          status: "sent",
          isRead: false,
          createdAt: FieldValue.serverTimestamp(),
        });
      } catch (err) {
        // Notification already persisted by client/callable; onBroadcastNotificationCreated handles push
      }
    }

    // 3. Notify customer when order fulfillment status changes
    if (afterData.status && beforeData.status !== afterData.status) {
      const statusText = String(afterData.status).slice(0, 60);
      const statusNotifId = `notif_order_status_${orderId}_${Date.now()}`;
      const statusTitle = `შეკვეთის სტატუსი განახლდა (${orderNum})`;
      const statusBody = `თქვენი ${orderNum} შეკვეთის სტატუსია: ${statusText}`;
      await dispatchCustomerUserPush({
        userId,
        notificationId: statusNotifId,
        title: statusTitle,
        body: statusBody,
        type: "order_update",
        targetTab: 4,
        targetProductId: 0,
        orderId,
        orderNumber: orderNum,
      });
    }
  }
);

/**
 * Callable endpoint: Returns bank transfer instructions exclusively for an authenticated
 * registered customer's own order (or the sole authorized administrator).
 */
exports.getOrderPaymentInstructions = onCall(async (request) => {
  const callerUid = requireRegisteredAuth(request);
  const callerEmail = normalizeEmail(request.auth.token && request.auth.token.email);
  const isAdminCaller =
    callerEmail === SOLE_AUTHORIZED_ADMIN_EMAIL &&
    request.auth.token &&
    request.auth.token.admin === true;

  const data = request.data && typeof request.data === "object" ? request.data : {};
  const orderId = typeof data.orderId === "string" ? data.orderId.trim() : "";
  if (!orderId) {
    throw new HttpsError("invalid-argument", "Valid orderId is required.");
  }

  const orderSnap = await db.collection("orders").doc(orderId).get();
  if (!orderSnap.exists) {
    throw new HttpsError("not-found", "Order not found.");
  }

  const orderData = orderSnap.data() || {};
  if (orderData.userId !== callerUid && !isAdminCaller) {
    throw new HttpsError(
      "permission-denied",
      "You are only authorized to view payment instructions for your own order."
    );
  }

  if (orderData.paymentMethodCode && orderData.paymentMethodCode !== "bank_transfer") {
    throw new HttpsError(
      "failed-precondition",
      "This order does not use the bank transfer payment method."
    );
  }

  return {
    orderId,
    orderNumber: orderData.orderNumber || orderId,
    totalAmount: orderData.totalAmount || 0,
    price: orderData.price || "0 ₾",
    currency: orderData.currency || "GEL",
    paymentStatus: orderData.paymentStatus || "pending",
    bankAccountId: orderData.bankAccountId || "",
    bankCode: orderData.bankCode || "",
    bankName: orderData.bankName || "",
    recipientName: orderData.recipientNameSnapshot || "BSTORE",
    iban: orderData.ibanSnapshot || "",
    paymentReference: orderData.paymentReference || orderData.orderNumber || orderId,
    paymentInstructions:
      orderData.paymentInstructionsSnapshot ||
      "გადარიცხვისას დანიშნულებაში მიუთითეთ შეკვეთის ნომერი.",
  };
});

/**
 * Callable endpoint (Admin-Only): Confirms either a Bank Transfer payment or a
 * Cash on Delivery (COD) collection after verification by the sole administrator,
 * updates `/orders/{orderId}`, and writes an immutable record to `/payment_audit_logs`.
 */
exports.adminConfirmOrderPayment = onCall(async (request) => {
  const callerUid = requireRegisteredAuth(request);
  const isSoleAdmin = await verifyAndEnsureSoleAdminUid(
    callerUid,
    request.auth.token
  );
  if (!isSoleAdmin) {
    throw new HttpsError(
      "permission-denied",
      "Only the sole authorized administrator (beqa1994@gmail.com) may confirm order payments."
    );
  }

  const data = request.data && typeof request.data === "object" ? request.data : {};
  const orderId = typeof data.orderId === "string" ? data.orderId.trim() : "";
  const internalNote =
    typeof data.internalNote === "string" ? data.internalNote.trim().slice(0, 500) : "";

  if (!orderId) {
    throw new HttpsError("invalid-argument", "orderId is required.");
  }

  const orderRef = db.collection("orders").doc(orderId);
  const orderSnap = await orderRef.get();
  if (!orderSnap.exists) {
    throw new HttpsError("not-found", "Order not found.");
  }

  const orderData = orderSnap.data() || {};
  const previousPaymentStatus = orderData.paymentStatus || "pending";
  const paymentMethodCode = orderData.paymentMethodCode || "bank_transfer";
  const nowMs = Date.now();
  const auditId = `audit_${nowMs}_${orderId.replace(/[^A-Za-z0-9_-]/g, "").slice(-10)}`;
  const auditRef = db.collection("payment_audit_logs").doc(auditId);

  const batch = db.batch();
  batch.update(orderRef, {
    paymentStatus: "paid",
    paymentConfirmedAtMillis: nowMs,
    paymentConfirmedBy: SOLE_AUTHORIZED_ADMIN_EMAIL,
    paymentInternalNote: internalNote,
    priceVerified: true,
    updatedAt: FieldValue.serverTimestamp(),
  });

  batch.set(auditRef, {
    id: auditId,
    orderId,
    orderNumber: orderData.orderNumber || orderId,
    eventType:
      paymentMethodCode === "cod"
        ? "COD_COLLECTION_CONFIRMED"
        : "BANK_TRANSFER_CONFIRMED",
    previousStatus: previousPaymentStatus,
    newStatus: "paid",
    amount: String(orderData.price || orderData.totalAmount || "0 ₾"),
    currency: orderData.currency || "GEL",
    actorUid: callerUid,
    actorEmail: SOLE_AUTHORIZED_ADMIN_EMAIL,
    actorRole: "sole_admin",
    timestamp: nowMs,
    note:
      internalNote ||
      (paymentMethodCode === "cod"
        ? "ნაღდი ანგარიშსწორება მიღებულია"
        : `საბანკო ჩარიცხვა დადასტურდა (${orderData.bankName || ""})`),
    createdAt: FieldValue.serverTimestamp(),
  });

  await batch.commit();

  return {
    success: true,
    orderId,
    paymentStatus: "paid",
    paymentConfirmedAtMillis: nowMs,
    paymentConfirmedBy: SOLE_AUTHORIZED_ADMIN_EMAIL,
    auditId,
  };
});

/**
 * Callable endpoint (Admin-Only): Creates or updates a destination bank account in
 * `/payment_bank_accounts/{bankAccountId}` with strict Georgian IBAN validation.
 */
exports.adminManageBankAccount = onCall(async (request) => {
  const callerUid = requireRegisteredAuth(request);
  const isSoleAdmin = await verifyAndEnsureSoleAdminUid(
    callerUid,
    request.auth.token
  );
  if (!isSoleAdmin) {
    throw new HttpsError(
      "permission-denied",
      "Only the sole authorized administrator (beqa1994@gmail.com) may manage bank accounts."
    );
  }

  const data = request.data && typeof request.data === "object" ? request.data : {};
  const id = typeof data.id === "string" ? data.id.trim() : "";
  const bankCode = typeof data.bankCode === "string" ? data.bankCode.trim().toLowerCase() : "";
  const bankName = typeof data.bankName === "string" ? data.bankName.trim() : "";
  const recipientName = typeof data.recipientName === "string" ? data.recipientName.trim() : "";
  const iban = typeof data.iban === "string" ? data.iban.replace(/\s+/g, "").toUpperCase() : "";
  const enabled = data.enabled !== false;
  const sortOrder = typeof data.sortOrder === "number" ? data.sortOrder : 0;
  const paymentInstructions =
    typeof data.paymentInstructions === "string" ? data.paymentInstructions.trim() : "";

  if (!id || !bankCode || !bankName || !recipientName) {
    throw new HttpsError("invalid-argument", "Missing required bank account fields.");
  }

  if (!/^GE[0-9]{2}[A-Z0-9]{2}[0-9]{16}$/.test(iban)) {
    throw new HttpsError("invalid-argument", "Invalid Georgian IBAN format.");
  }

  await db.collection("payment_bank_accounts").doc(id).set(
    {
      id,
      bankCode,
      bankName,
      recipientName,
      iban,
      currency: "GEL",
      enabled: Boolean(enabled),
      sortOrder,
      paymentInstructions,
      updatedAtMillis: Date.now(),
      updatedAt: FieldValue.serverTimestamp(),
    },
    { merge: true }
  );

  return {
    success: true,
    id,
    bankCode,
    bankName,
    iban,
    enabled: Boolean(enabled),
  };
});

/**
 * Callable endpoint (Order Owner Only): Records that the customer clicked
 * "✓ თანხა გადავრიცხე" on a Bank Transfer order while status is still `pending`.
 * - Strictly forbids setting `paid` from the client.
 * - Verifies ownership, payment method, and active order status.
 * - If a different enabled bank account ID is passed, resolves its real snapshot
 *   from `/payment_bank_accounts`.
 */
exports.customerConfirmBankTransferSent = onCall(async (request) => {
  const callerUid = requireRegisteredAuth(request);
  const data = request.data && typeof request.data === "object" ? request.data : {};
  const orderId = typeof data.orderId === "string" ? data.orderId.trim() : "";
  const bankAccountId =
    typeof data.bankAccountId === "string" ? data.bankAccountId.trim() : "";

  if (!orderId) {
    throw new HttpsError("invalid-argument", "orderId is required.");
  }

  const orderRef = db.collection("orders").doc(orderId);
  const orderSnap = await orderRef.get();
  if (!orderSnap.exists) {
    throw new HttpsError("not-found", "Order not found.");
  }

  const orderData = orderSnap.data() || {};
  if (orderData.userId !== callerUid) {
    throw new HttpsError(
      "permission-denied",
      "You are only authorized to confirm transfer for your own order."
    );
  }

  if (orderData.paymentMethodCode !== "bank_transfer") {
    throw new HttpsError(
      "failed-precondition",
      "Only bank transfer orders can be marked as transfer sent."
    );
  }

  if (orderData.status === "cancelled" || orderData.status === "completed") {
    throw new HttpsError(
      "failed-precondition",
      "Cannot modify payment on a completed or cancelled order."
    );
  }

  const currentPaymentStatus = orderData.paymentStatus || "pending";
  if (currentPaymentStatus === "verification_pending") {
    return {
      success: true,
      alreadyPendingVerification: true,
      orderId,
      paymentStatus: "verification_pending",
      userConfirmedPaymentAtMillis: orderData.userConfirmedPaymentAtMillis || Date.now(),
    };
  }

  if (currentPaymentStatus !== "pending") {
    throw new HttpsError(
      "failed-precondition",
      `Transfer confirmation is not allowed in status: ${currentPaymentStatus}`
    );
  }

  const updates = {
    paymentStatus: "verification_pending",
    userConfirmedPaymentAtMillis: Date.now(),
    updatedAt: FieldValue.serverTimestamp(),
  };

  if (bankAccountId && bankAccountId !== orderData.bankAccountId) {
    const bankSnap = await db.collection("payment_bank_accounts").doc(bankAccountId).get();
    if (!bankSnap.exists || bankSnap.data()?.enabled !== true) {
      throw new HttpsError(
        "failed-precondition",
        "Selected bank account is not available."
      );
    }
    const bankData = bankSnap.data();
    updates.bankAccountId = bankAccountId;
    updates.bankCode = bankData.bankCode || "";
    updates.bankName = bankData.bankName || "";
    updates.recipientNameSnapshot = bankData.recipientName || "";
    updates.ibanSnapshot = bankData.iban || "";
    updates.paymentMethod = `საბანკო გადარიცხვა (${bankData.bankName || ""})`;
    updates.paymentInstructionsSnapshot = bankData.paymentInstructions || "";
  }

  await orderRef.update(updates);

  return {
    success: true,
    orderId,
    paymentStatus: "verification_pending",
    userConfirmedPaymentAtMillis: updates.userConfirmedPaymentAtMillis,
  };
});


