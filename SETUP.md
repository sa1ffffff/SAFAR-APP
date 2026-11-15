# Safar setup

What you need to do outside the code, in order. Project: **safarbysaif** (Firebase) / the same project in Google Cloud.

## Already done

- Realtime Database rules and Storage rules from this repo (`database.rules.json`, `storage.rules`) are deployed.
- `/userConversations` was backfilled for the 10 existing conversations, so old chats show up in the new inbox.
- The debug keystore's SHA-1 and SHA-256 are registered on the Android app.

## Costs, briefly

Sign-in is **Google only**, which is free, so there are no SMS charges. On Blaze with about 1,000 users, expect roughly $0–$5 a month (database downloads, the stored function image). Set a budget alert anyway under Firebase console → ⚙️ → **Usage and billing → Details & settings**.

## 1. Authentication (Firebase console)

Firebase console → **Authentication → Sign-in method**

1. **Google**: make sure it's enabled and has a support email set. This is the only way to sign in.
2. **Phone**: **disable** it. The app no longer signs in by phone; it asks for the number after Google sign-in and stores it on the profile. Leaving the provider on would let anyone trigger paid SMS through the API.
3. **Email/Password**: the app doesn't use it, so disable it too.

Old accounts that signed in by phone get a new, empty account if they now sign in with Google; their phone account can't be reached any more. Before launch that's only a handful of test accounts.

Old accounts that signed in with phone + password stored a password hash in `/users/{uid}/passwordHash`. The new app ignores it. To clean up, delete the `passwordHash`, `resetCode` and `resetCodeExpiry` children under each user in **Realtime Database → Data**.

## 2. Play Integrity API, for App Check (Google Cloud console)

Google Cloud console → select project **safarbysaif** → **APIs & Services → Library**

1. Search for **Play Integrity API** and click **Enable**. App Check (section 6) needs it to vouch for release builds.

## 3. Lock down the Maps key (Google Cloud console). Important

The Maps key `AIzaSyAVFulc…` is in your public GitHub history (old commits included build output). Restrict it now:

Google Cloud console → **APIs & Services → Credentials** → click the Maps key

1. **Application restrictions** → **Android apps** → **Add** → package `com.safar.app`, plus the SHA-1 of your debug keystore (and your release keystore once you have one; see section 5).
2. **API restrictions** → **Restrict key** → tick only **Maps SDK for Android**.
3. Save. It takes a few minutes to apply.

Safer option: click **Rotate key** (or create a new key with the same restrictions), put the new value in `local.properties` as `MAPS_API_KEY=...`, then delete the old key.

The key now comes from `local.properties`, which git ignores:

```
MAPS_API_KEY=your-key-here
```

The old EmailJS key (`cPZ4ROW8…`) is also in the history. The new app doesn't use EmailJS. If that EmailJS account is still active, regenerate its key in the EmailJS dashboard.

## 4. Profile photos (Cloud Storage)

The bucket `safarbysaif.firebasestorage.app` was created after October 2024. Google requires the **Blaze** plan for these buckets. If uploading a profile photo fails with a billing or permission error, upgrade the project:

Firebase console → ⚙️ → **Usage and billing** → **Modify plan** → **Blaze**. Set a budget alert (for example $1) under **Details & settings**. At this app's size, usage stays inside the free allowance.

## 5. Release signing (for the Play Store or a shareable APK)

Create a keystore once and keep it backed up. You can't update the app without it.

```bash
keytool -genkey -v -keystore "E:/keystore safar/safar-release.jks" -keyalg RSA -keysize 2048 -validity 10000 -alias safar
```

Then create `keystore.properties` in the project root (git ignores it):

```
storeFile=E:/keystore safar/safar-release.jks
storePassword=...
keyAlias=safar
keyPassword=...
```

Get its fingerprints:

```bash
keytool -list -v -keystore "E:/keystore safar/safar-release.jks" -alias safar
```

Add the **SHA-1** and **SHA-256** in Firebase console → ⚙️ Project settings → **Your apps → com.safar.app → Add fingerprint**. Google sign-in fails on release builds without this. Also add the SHA-1 to the Maps key's Android restriction (section 3).

## 6. App Check (blocks bots and scripts from hitting the database and storage)

The app already sends App Check tokens: Play Integrity in release builds, the debug provider in debug builds. Enforcement is still **off**. Turn it on like this:

1. Firebase console → **App Check → Apps** → `com.safar.app` → **Play Integrity** → register it with the **SHA-256** of your release key. If you publish on Google Play, use the **app signing key** SHA-256 from Play Console → *Test and release → App integrity*.
2. Run a debug build once and search logcat for `DebugAppCheckProvider`. Copy the debug secret it prints into **App Check → Apps → ⋮ → Manage debug tokens**.
3. Leave it unenforced for a few days. Under **App Check → APIs**, watch the request metrics until nearly all traffic is "verified".
4. Click **Enforce** for **Realtime Database**, **Cloud Storage** and **Authentication**.

## 7. Crashlytics

Nothing to set up. Open Firebase console → **Crashlytics** once so it starts collecting. Release builds are minified, and the Gradle plugin uploads the mapping file so stack traces stay readable.

## 8. Cloud Functions (needs Blaze)

`functions/index.js` has two functions:

- `onNotificationCreated` sends a system push whenever the app writes to `/notifications/{uid}`. Without it, notifications appear only inside the app.
- `purgeExpiredRides` runs daily at 03:30 (Pakistan time). It deletes expired rides nobody booked, and keeps booked ones for 14 days as completed.

Once the project is on Blaze, deploy them:

```bash
npm install -g firebase-tools
firebase login
cd "E:/SAFAR ktjava/functions" && npm install
cd .. && firebase deploy --only functions
```

## 9. Reviewing reports

Reports land in Realtime Database under `/reports`. Nobody can read them from the app. Check them in the Firebase console.

## Building

```bash
./gradlew :app:assembleDebug
```

Open the project in Android Studio and run on a device or emulator with Google Play services.
