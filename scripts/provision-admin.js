/**
 * BEKASERVICE — Trusted Server-Side Administrator Custom Claim Provisioning Script
 *
 * This script runs in a trusted server environment using the Firebase Admin SDK to:
 * 1. Connect to the designated Firebase project ("bekaservice-cc917" or process.env.FIREBASE_PROJECT_ID).
 * 2. Locate the Firebase Auth user record by the exact authorized email: "beqa1994@gmail.com".
 * 3. Verify that the account exists and check email verification status.
 * 4. Retrieve the verified user UID.
 * 5. Read existing custom claims and preserve all unrelated claims.
 * 6. Set custom claim { admin: true, role: "sole_admin" }.
 * 7. Commit changes to Firebase Authentication via admin.auth().setCustomUserClaims().
 * 8. Re-fetch user record to verify the saved custom claims on the server.
 * 9. Synchronize the active administrator document in Firestore: `/admins/{uid}`.
 *
 * Security Invariants:
 * - Operates EXCLUSIVELY on "beqa1994@gmail.com" (strictly refuses any other email).
 * - Never prints passwords, private keys, refresh tokens, or bearer tokens.
 * - Non-destructive: preserves existing claims and user profile data.
 *
 * Usage:
 *   node scripts/provision-admin.js [optional_path_to_service_account.json]
 *   Or with GOOGLE_APPLICATION_CREDENTIALS=/path/to/service-account.json node scripts/provision-admin.js
 */

const fs = require('fs');
const path = require('path');

const SOLE_ADMIN_EMAIL = 'beqa1994@gmail.com';
const TARGET_PROJECT_ID = process.env.FIREBASE_PROJECT_ID || 'bekaservice-cc917';

async function main() {
  console.log('================================================================');
  console.log('BEKASERVICE — FIREBASE ADMIN CLAIM PROVISIONING');
  console.log('Target Project:', TARGET_PROJECT_ID);
  console.log('Target Sole Admin:', SOLE_ADMIN_EMAIL);
  console.log('================================================================');

  let admin;
  try {
    admin = require('firebase-admin');
  } catch (err) {
    try {
      admin = require(path.join(__dirname, '../functions/node_modules/firebase-admin'));
    } catch (e) {
      console.error('❌ Error: firebase-admin module not found.');
      console.error('Run: npm install firebase-admin in the project root or functions/ directory.');
      process.exit(1);
    }
  }

  // Initialize Firebase Admin SDK
  let serviceAccountKeyPath = process.argv[2] || process.env.GOOGLE_APPLICATION_CREDENTIALS;
  if (!serviceAccountKeyPath) {
    const candidatePaths = [
      path.join(__dirname, '../service-account.json'),
      path.join(__dirname, '../bekaservice-service-account.json'),
      path.join(__dirname, '../functions/service-account.json'),
    ];
    for (const p of candidatePaths) {
      if (fs.existsSync(p)) {
        serviceAccountKeyPath = p;
        break;
      }
    }
  }

  if (serviceAccountKeyPath && fs.existsSync(serviceAccountKeyPath)) {
    console.log(`Using Service Account credentials from: ${path.basename(serviceAccountKeyPath)}`);
    const serviceAccount = JSON.parse(fs.readFileSync(serviceAccountKeyPath, 'utf8'));
    admin.initializeApp({
      credential: admin.credential.cert(serviceAccount),
      projectId: serviceAccount.project_id || TARGET_PROJECT_ID,
    });
  } else {
    console.log('Using Application Default Credentials (ADC) or Environment credentials...');
    admin.initializeApp({
      projectId: TARGET_PROJECT_ID,
    });
  }

  const auth = admin.auth();
  const db = admin.firestore();

  try {
    // 1. Locate user by exact email
    console.log(`\n[Step 1] Locating Firebase Auth user for ${SOLE_ADMIN_EMAIL}...`);
    let userRecord;
    try {
      userRecord = await auth.getUserByEmail(SOLE_ADMIN_EMAIL);
    } catch (err) {
      if (err.code === 'auth/user-not-found') {
        console.error(`❌ User '${SOLE_ADMIN_EMAIL}' was not found in Firebase project '${TARGET_PROJECT_ID}'.`);
        console.error('Please create or sign in with this Google account in the app first so the user record exists.');
        process.exit(1);
      }
      throw err;
    }

    const uid = userRecord.uid;
    console.log(`✅ Found user: UID = ${uid}, EmailVerified = ${userRecord.emailVerified}, Disabled = ${userRecord.disabled}`);

    if (userRecord.disabled) {
      console.error('❌ User account is disabled in Firebase Authentication.');
      process.exit(1);
    }

    // 2. Read existing custom claims and preserve unrelated claims
    console.log('\n[Step 2] Reading existing custom claims...');
    const currentClaims = userRecord.customClaims || {};
    console.log('Current claims:', JSON.stringify(currentClaims));

    // 3. Build updated claims preserving all unrelated fields
    const updatedClaims = {
      ...currentClaims,
      admin: true,
      role: 'sole_admin',
      adminEmail: SOLE_ADMIN_EMAIL,
    };

    // 4. Set custom claims using Firebase Admin SDK
    console.log('\n[Step 3] Assigning Custom Claim { admin: true } via Firebase Admin SDK...');
    await auth.setCustomUserClaims(uid, updatedClaims);
    console.log('✅ Custom claims successfully written to Firebase Authentication.');

    // 5. Re-fetch user and verify saved claims
    console.log('\n[Step 4] Verifying updated claims on Firebase Auth server...');
    const verifiedUser = await auth.getUser(uid);
    const verifiedClaims = verifiedUser.customClaims || {};
    console.log('Verified claims:', JSON.stringify(verifiedClaims));

    if (verifiedClaims.admin !== true) {
      console.error('❌ Verification failed: custom claim "admin" is not true on the server.');
      process.exit(1);
    }
    console.log('✅ VERIFIED: Firebase Auth User now has { admin: true } custom claim!');

    // 6. Synchronize /admins/{uid} document in Firestore
    console.log(`\n[Step 5] Synchronizing Firestore document /admins/${uid}...`);
    try {
      await db.collection('admins').doc(uid).set(
        {
          uid,
          email: SOLE_ADMIN_EMAIL,
          enabled: true,
          role: 'sole_admin',
          customClaimAdmin: true,
          updatedAt: admin.firestore.FieldValue.serverTimestamp(),
        },
        { merge: true }
      );
      console.log(`✅ Document /admins/${uid} successfully synchronized in Firestore.`);
    } catch (dbErr) {
      console.warn(`⚠️ Warning: Could not write /admins/${uid} to Firestore directly: ${dbErr.message}`);
    }

    console.log('\n================================================================');
    console.log('🎉 SUCCESS: Administrator provisioning complete.');
    console.log(`User: ${SOLE_ADMIN_EMAIL} (UID: ${uid})`);
    console.log('Claims: admin=true, role=sole_admin');
    console.log('The Android application will now automatically recognize administrator');
    console.log('access as soon as the ID token is refreshed.');
    console.log('================================================================\n');
  } catch (err) {
    console.error('❌ Provisioning error:', err.code || err.name, err.message);
    process.exit(1);
  }
}

main();
