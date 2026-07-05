const functions = require("firebase-functions");
const admin = require("firebase-admin");
admin.initializeApp();

exports.sendCallNotification = functions.firestore
    .document("calls/{callId}")
    .onCreate(async (snapshot, context) => {
        const callData = snapshot.data();
        const callId = context.params.callId;

        if (!callData) {
            console.log("No call data found");
            return null;
        }

        const speakerId = callData.speakerId;
        const callerName = callData.callerName || "Someone";

        // Optimization: Use token from call data if available
        let fcmToken = callData.speakerFcmToken;

        try {
            if (!fcmToken) {
                console.log("No embedded FCM token, fetching from user doc...");
                // Fallback: Get the speaker's FCM token from DB
                const userDoc = await admin.firestore().collection("users").doc(speakerId).get();
                if (!userDoc.exists) {
                    console.log("Speaker not found");
                    return null;
                }

                const userData = userDoc.data();
                fcmToken = userData.fcmToken;
            } else {
                console.log("Using embedded FCM token.");
            }

            if (!fcmToken) {
                console.log("No FCM token for speaker");
                return null;
            }

            // Construct the Data Payload (High Priority)
            const payload = {
                data: {
                    type: "call",
                    callId: callId,
                    callerId: callData.callerId,
                    callerName: callerName,
                    callerAvatar: callData.callerAvatar || "",
                    rate: String(callData.rate),
                    callType: callData.type || "audio"
                },
                token: fcmToken,
                android: {
                    priority: "high",
                    ttl: 0 // Immediate delivery
                }
            };

            // Send the message
            const response = await admin.messaging().send(payload);
            console.log("Successfully sent message:", response);
            return response;

        } catch (error) {
            console.log("Error sending notification:", error);
            return null;
        }
    });

const Razorpay = require("razorpay");

// Initialize Razorpay with credentials from Firebase Config
// Ensure you set these: firebase functions:config:set razorpay.key_id="YOUR_KEY_ID" razorpay.key_secret="YOUR_KEY_SECRET"
const razorpay = new Razorpay({
    key_id: functions.config().razorpay?.key_id || "rzp_test_RQzTCSQezDt3qq",
    key_secret: functions.config().razorpay?.key_secret || "TUn0tMsECdh97s3t1kYHtADH"
});

exports.createRazorpayOrder = functions.https.onCall(async (data, context) => {
    // 1. Authentication Check
    if (!context.auth) {
        throw new functions.https.HttpsError(
            "unauthenticated",
            "The function must be called while authenticated."
        );
    }

    const userId = context.auth.uid;
    const amountInRupees = data.amount; // Expecting amount in Rupees (e.g. 100)

    // 2. Validation
    if (!amountInRupees || amountInRupees <= 0) {
        throw new functions.https.HttpsError(
            "invalid-argument",
            "The function must be called with a valid amount."
        );
    }

    // 3. Create Order
    try {
        const options = {
            amount: amountInRupees * 100, // Amount in paise
            currency: "INR",
            receipt: `rcpt_${userId.substring(0, 10)}_${Date.now()}`,
            notes: {
                userId: userId,
                description: "Voxly Coin Pack"
            }
        };

        const order = await razorpay.orders.create(options);

        console.log("Razorpay Order Created:", order.id);

        // Return the Order ID to the client
        return {
            orderId: order.id,
            amount: order.amount,
            currency: order.currency,
            keyId: razorpay.key_id
        };

    } catch (error) {
        console.error("Razorpay Order Creation Failed Full Error:", JSON.stringify(error, Object.getOwnPropertyNames(error)));

        const errorMessage = error.message || error.description || JSON.stringify(error) || "Unknown Error";

        throw new functions.https.HttpsError(
            "internal",
            "Unable to create Razorpay order: " + errorMessage
        );
    }
});

// Initialize Google APIs for Play Billing Verification
const { google } = require('googleapis');
const androidPublisher = google.androidpublisher('v3');

// You will need a Service Account JSON file for Google Play Developer API
// Save it to functions/play_service_account.json and add it to .gitignore
// For testing locally without it, you can mock the response or set it up later.
let authClient = null;
try {
    const keyFile = require('./play_service_account.json');
    authClient = new google.auth.JWT({
        email: keyFile.client_email,
        key: keyFile.private_key,
        scopes: ['https://www.googleapis.com/auth/androidpublisher']
    });
} catch (e) {
    console.warn("Play Service Account Key not found. Verification will fail in production.");
}

exports.verifyPlayPurchase = functions.https.onCall(async (data, context) => {
    // 1. Auth Check
    if (!context.auth) {
        throw new functions.https.HttpsError('unauthenticated', 'User must be logged in.');
    }
    const userId = context.auth.uid;
    const { purchaseToken, productId } = data;

    if (!purchaseToken || !productId) {
        throw new functions.https.HttpsError('invalid-argument', 'Missing token or productId.');
    }

    if (!authClient) {
        throw new functions.https.HttpsError('internal', 'Server not configured for Play Verification.');
    }

    try {
        // 2. Verify with Google Play
        const response = await androidPublisher.purchases.products.get({
            packageName: 'com.rkdevstudios.voxly', // Your App ID
            productId: productId,
            token: purchaseToken,
            auth: authClient
        });

        const purchaseState = response.data.purchaseState;
        const consumptionState = response.data.consumptionState;

        // purchaseState: 0 (Purchased), 1 (Canceled), 2 (Pending)
        if (purchaseState !== 0) {
            throw new functions.https.HttpsError('failed-precondition', 'Purchase is not fully complete.');
        }

        // 3. Map productId to coins dynamically from centralized configuration
        const productConfig = require('./products.json');
        const product = productConfig[productId];
        if (!product) {
            throw new functions.https.HttpsError('invalid-argument', 'Unknown productId.');
        }

        if (product.active === false) {
            throw new functions.https.HttpsError('failed-precondition', 'This product package is no longer active.');
        }

        const coinsToAdd = product.coins;
        const amountPaidInr = product.expectedPrice;

        // 4. Securely add coins in Firestore using a Transaction
        const db = admin.firestore();
        const userRef = db.collection('users').doc(userId);

        // Prevent replay attacks: ensure token isn't already used
        const txRef = db.collection('transactions').doc(purchaseToken);

        await db.runTransaction(async (transaction) => {
            const txDoc = await transaction.get(txRef);
            if (txDoc.exists) {
                throw new Error("Purchase already processed.");
            }

            const userDoc = await transaction.get(userRef);
            const currentCoins = userDoc.data()?.coins || 0;

            // Add Coins
            transaction.update(userRef, { coins: currentCoins + coinsToAdd });

            // Record Transaction
            transaction.set(txRef, {
                id: purchaseToken,
                userId: userId,
                amount: amountPaidInr,
                coins: coinsToAdd,
                description: `Play Store Purchase (${productId})`,
                status: 'success',
                timestamp: admin.firestore.FieldValue.serverTimestamp(),
                provider: 'google_play',
                pricingVersion: product.pricingVersion || 1
            });
        });

        return { success: true, coinsAdded: coinsToAdd };

    } catch (error) {
        console.error("Play Verification Error:", error);
        throw new functions.https.HttpsError('internal', error.message || 'Verification failed.');
    }
});

exports.onDeletionRequest = functions.firestore
    .document("accountDeletionRequests/{userId}")
    .onCreate(async (snapshot, context) => {
        const userId = context.params.userId;
        const db = admin.firestore();
        const requestRef = db.collection("accountDeletionRequests").doc(userId);

        console.log(`Received account deletion request for user: ${userId}`);

        // 1. Transactional Idempotency & Request Integrity Guard
        let retryCount = 0;
        try {
            const shouldProcess = await db.runTransaction(async (transaction) => {
                const requestDoc = await transaction.get(requestRef);
                if (!requestDoc.exists) {
                    console.log("Request document does not exist");
                    return false;
                }

                const requestData = requestDoc.data();
                
                // Request Integrity Validation
                if (requestData.userId !== userId) {
                    console.log("Trigger document ID does not match stored userId");
                    transaction.update(requestRef, {
                        status: "failed",
                        error: "Request Integrity Validation failed: document ID mismatch",
                        failedAt: admin.firestore.FieldValue.serverTimestamp()
                    });
                    return false;
                }

                if (requestData.status !== "pending") {
                    console.log(`Request is already in status: ${requestData.status}`);
                    return false;
                }

                retryCount = requestData.retryCount || 0;

                // Move status to processing
                transaction.update(requestRef, { status: "processing" });
                return true;
            });

            if (!shouldProcess) {
                return null;
            }
        } catch (error) {
            console.error("Error running transactional status guard:", error);
            return null;
        }

        // 2. Idempotent Execution Pipeline
        try {
            // A. Storage cleanup (delete avatars/{userId})
            try {
                const bucket = admin.storage().bucket();
                const file = bucket.file(`avatars/${userId}`);
                const [exists] = await file.exists();
                if (exists) {
                    await file.delete();
                    console.log(`Deleted Storage avatar for user: ${userId}`);
                }
            } catch (storageError) {
                console.warn(`Non-blocking Storage cleanup warning for ${userId}:`, storageError.message);
            }

            // B. Anonymize matching documents in the calls collection
            try {
                const callsRef = db.collection("calls");
                const callerCalls = await callsRef.where("callerId", "==", userId).get();
                const speakerCalls = await callsRef.where("speakerId", "==", userId).get();

                const batch = db.batch();
                callerCalls.forEach(doc => {
                    batch.update(doc.ref, { callerId: "deleted_user" });
                });
                speakerCalls.forEach(doc => {
                    batch.update(doc.ref, { speakerId: "deleted_user" });
                });
                await batch.commit();
                console.log(`Anonymized call documents for user: ${userId}`);
            } catch (callsError) {
                console.error("Calls anonymization error:", callsError.message);
                throw callsError;
            }

            // C. Anonymize user references in the reports collection
            try {
                const reportsRef = db.collection("reports");
                const reportsCreated = await reportsRef.where("reporterId", "==", userId).get();
                const reportsAgainst = await reportsRef.where("reportedId", "==", userId).get();

                const batch = db.batch();
                reportsCreated.forEach(doc => {
                    batch.update(doc.ref, { reporterId: "deleted_user" });
                });
                reportsAgainst.forEach(doc => {
                    batch.update(doc.ref, { reportedId: "deleted_user" });
                });
                await batch.commit();
                console.log(`Anonymized reports for user: ${userId}`);
            } catch (reportsError) {
                console.error("Reports anonymization error:", reportsError.message);
                throw reportsError;
            }

            // D. Delete the Firestore profile document
            try {
                const userRef = db.collection("users").doc(userId);
                const userDoc = await userRef.get();
                if (userDoc.exists) {
                    await userRef.delete();
                    console.log(`Deleted Firestore user profile document for: ${userId}`);
                }
            } catch (profileError) {
                console.error("Profile document deletion error:", profileError.message);
                throw profileError;
            }

            // E. Delete the Firebase Auth user
            try {
                await admin.auth().deleteUser(userId);
                console.log(`Deleted Auth user account for: ${userId}`);
            } catch (authError) {
                // If user is already deleted from Auth, we treat it as success
                if (authError.code !== "auth/user-not-found") {
                    console.error("Auth account deletion error:", authError.message);
                    throw authError;
                }
            }

            // 3. Mark completed and set 90 days TTL deleteAt timestamp
            const retentionDays = 90;
            const deleteAt = new Date();
            deleteAt.setDate(deleteAt.getDate() + retentionDays);

            await requestRef.update({
                status: "completed",
                processedAt: admin.firestore.FieldValue.serverTimestamp(),
                deleteAt: admin.firestore.Timestamp.fromDate(deleteAt)
            });

            console.log(`Account deletion request successfully completed for user: ${userId}`);

        } catch (pipelineError) {
            console.error("Account deletion pipeline encountered error:", pipelineError);
            await requestRef.update({
                status: "failed",
                error: pipelineError.message || "Unknown pipeline error",
                failedAt: admin.firestore.FieldValue.serverTimestamp(),
                retryCount: retryCount + 1
            });
        }

        return null;
    });
