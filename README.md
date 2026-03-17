# SAFAR — Community Carpool Marketplace
### Android MVP · Java OOP · Firebase · Google Maps

---

## Table of Contents
1. [Project Overview](#1-project-overview)
2. [Architecture](#2-architecture)
3. [Project Structure](#3-project-structure)
4. [Prerequisites](#4-prerequisites)
5. [Firebase Setup (Required First)](#5-firebase-setup-required-first)
6. [Google Maps Setup](#6-google-maps-setup)
7. [Build & Run](#7-build--run)
8. [App Flow & Features](#8-app-flow--features)
9. [Firebase Database Schema](#9-firebase-database-schema)
10. [Deploying to Google Play Store](#10-deploying-to-google-play-store)
11. [Scaling Roadmap](#11-scaling-roadmap)
12. [Troubleshooting](#12-troubleshooting)

---

## 1. Project Overview

SAFAR is a peer-to-peer community carpool marketplace for Pakistan. Users can:
- **Post** ride ads (e.g., "Lahore → Islamabad, Friday 8 AM, 2 seats")
- **Browse** rides on a Yango-style dark map or searchable list
- **Request** to join a ride
- **Chat** in real-time after request acceptance
- **Coordinate** payment offline (cash/bank transfer) — no in-app payments

**Tech Stack:**
| Layer | Technology |
|---|---|
| Language | Java (JDK 11) |
| Architecture | MVP (Model-View-Presenter) |
| UI | XML Layouts + Material Design 3 |
| Database | Firebase Realtime Database |
| Auth | Firebase Phone Auth (OTP) |
| Maps | Google Maps SDK for Android |
| Push | Firebase Cloud Messaging (FCM) |

---

## 2. Architecture

```
com.safar.app/
├── models/          → Pure data classes (Firebase-serializable POJOs)
├── presenters/      → Business logic, Firebase listeners
├── views/
│   ├── activities/  → Android Activities (View layer)
│   ├── fragments/   → Map, List, Inbox, BottomSheet fragments
│   └── adapters/    → RecyclerView adapters
├── firebase/        → FirebaseManager singleton, FCM service
└── utils/           → AppConstants, DateUtils, SessionManager
```

**MVP Pattern:**
- **Model** — `RideAd`, `User`, `ChatMessage`, `JoinRequest`
- **View** — Activities/Fragments that implement View interfaces
- **Presenter** — `AdsPresenter`, `ChatPresenter`, `RequestPresenter` containing all Firebase calls and logic

---

## 3. Project Structure

```
SAFAR/
├── app/
│   ├── src/main/
│   │   ├── java/com/safar/app/
│   │   │   ├── models/
│   │   │   │   ├── RideAd.java
│   │   │   │   ├── User.java
│   │   │   │   ├── ChatMessage.java
│   │   │   │   └── JoinRequest.java
│   │   │   ├── presenters/
│   │   │   │   ├── AdsPresenter.java
│   │   │   │   ├── ChatPresenter.java
│   │   │   │   └── RequestPresenter.java
│   │   │   ├── views/
│   │   │   │   ├── activities/ (10 activities)
│   │   │   │   ├── fragments/  (4 fragments)
│   │   │   │   └── adapters/   (5 adapters)
│   │   │   ├── firebase/
│   │   │   │   ├── FirebaseManager.java
│   │   │   │   └── SafarMessagingService.java
│   │   │   └── utils/
│   │   │       ├── AppConstants.java
│   │   │       ├── DateUtils.java
│   │   │       └── SessionManager.java
│   │   ├── res/
│   │   │   ├── layout/  (15 XML layouts)
│   │   │   ├── drawable/ (12 drawables + icons)
│   │   │   ├── values/  (colors, strings, themes, dimens)
│   │   │   ├── menu/    (bottom_nav_menu.xml)
│   │   │   ├── color/   (nav_item_color.xml)
│   │   │   └── raw/     (map_style.json)
│   │   └── AndroidManifest.xml
│   ├── google-services.json  ← REPLACE WITH YOUR OWN
│   ├── build.gradle
│   └── proguard-rules.pro
├── build.gradle
├── settings.gradle
├── gradle.properties
├── firebase_database_rules.json
└── README.md
```

---

## 4. Prerequisites

| Tool | Version | Download |
|---|---|---|
| Android Studio | Hedgehog 2023.1+ | https://developer.android.com/studio |
| JDK | 11+ | Bundled with Android Studio |
| Android SDK | API 34 | Via SDK Manager in Android Studio |
| Min Android | API 24 (Android 7.0) | — |
| Google Account | — | For Firebase & Play Console |

---

## 5. Firebase Setup (Required First)

> ⚠️ The app will NOT build without a real `google-services.json`.

### Step-by-step:

**A. Create Firebase Project**
1. Go to https://console.firebase.google.com
2. Click **"Add Project"** → Name it `SAFAR`
3. Disable Google Analytics (optional for MVP)

**B. Add Android App**
1. In Firebase Console → Click the **Android icon** `</>`
2. **Package name:** `com.safar.app`
3. **App nickname:** SAFAR
4. **SHA-1:** Get it by running in terminal:
   ```bash
   # On Mac/Linux:
   keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
   
   # On Windows:
   keytool -list -v -keystore "%USERPROFILE%\.android\debug.keystore" -alias androiddebugkey -storepass android -keypass android
   ```
5. Download `google-services.json`
6. **Replace** `app/google-services.json` with this downloaded file

**C. Enable Authentication**
1. Firebase Console → **Authentication** → Get Started
2. Click **Phone** → Enable it → Save

**D. Create Realtime Database**
1. Firebase Console → **Realtime Database** → Create Database
2. Choose region (closest to Pakistan = **us-central1** or **asia-southeast1**)
3. Start in **test mode** initially
4. After testing, go to **Rules** tab and paste contents of `firebase_database_rules.json`

**E. Enable Cloud Messaging (FCM)**
1. Firebase Console → **Project Settings** → **Cloud Messaging**
2. FCM is enabled by default — no extra steps needed

---

## 6. Google Maps Setup

1. Go to https://console.cloud.google.com
2. Create a project (or use the same Firebase project)
3. Enable **Maps SDK for Android**
4. Go to **Credentials** → **Create Credentials** → **API Key**
5. Restrict the key to **Android apps** with your package name + SHA-1
6. Open `AndroidManifest.xml`, find:
   ```xml
   <meta-data android:name="com.google.android.geo.API_KEY"
              android:value="YOUR_GOOGLE_MAPS_API_KEY" />
   ```
7. Replace `YOUR_GOOGLE_MAPS_API_KEY` with your real key

---

## 7. Build & Run

### In Android Studio:
```
1. File → Open → Select the SAFAR/ folder
2. Wait for Gradle sync to complete (~2-3 min first time)
3. Connect an Android device or start an emulator (API 24+)
4. Press ▶ Run (Shift+F10)
```

### From command line:
```bash
cd SAFAR/
./gradlew assembleDebug         # Build debug APK
./gradlew installDebug          # Build + install on connected device
./gradlew assembleRelease       # Build release APK (needs signing)
```

### Debug APK location after build:
```
app/build/outputs/apk/debug/app-debug.apk
```

---

## 8. App Flow & Features

```
Launch
  └─► SplashActivity (1.8s)
        ├─► Not logged in → PhoneAuthActivity → OtpVerifyActivity
        └─► Logged in
              ├─► Safety not shown → SafetyActivity (one-time)
              └─► Safety shown → MainActivity
```

### MainActivity (Bottom Nav):
| Tab | Fragment | Description |
|---|---|---|
| 🗺 Map | MapFragment | Dark map with yellow pins per ride |
| 🚗 Rides | ListFragment | Searchable list of all open rides |
| 📬 Inbox | InboxFragment | Join requests received by ad owner |
| 👤 Profile | ProfileActivity | Stats, My Rides, Logout |

### Ride Lifecycle:
```
User A posts RideAd (status: OPEN)
  └─► User B sees pin on map / list item
        └─► Taps → Bottom sheet or AdDetailActivity
              └─► Taps "Request to Join"
                    └─► JoinRequest created (status: PENDING)
                          └─► User A sees in Inbox
                                ├─► Accepts → ChatRoom created, both users enter chat
                                └─► Rejects → Request marked REJECTED
```

### Chat Features:
- Real-time Firebase listener (no polling)
- Safety disclaimer banner always visible
- Sent/received bubble styling (yellow / dark)
- System messages (e.g., "Request accepted")

---

## 9. Firebase Database Schema

```
/
├── users/
│   └── {uid}/
│       ├── uid: string
│       ├── name: string
│       ├── phone: string
│       ├── joinedAt: timestamp
│       ├── totalRides: int
│       ├── rating: double
│       ├── ratingCount: int
│       └── fcmToken: string
│
├── ads/
│   └── {adId}/
│       ├── adId: string
│       ├── creatorUid: string
│       ├── creatorName: string
│       ├── origin: string
│       ├── destination: string
│       ├── originLat / originLng: double
│       ├── destinationLat / destinationLng: double
│       ├── departureTime: timestamp (ms)
│       ├── availableSeats: int
│       ├── vehicleDetails: string (nullable)
│       ├── notes: string (nullable)
│       ├── status: "OPEN" | "FILLED" | "COMPLETED" | "CANCELLED"
│       └── createdAt: timestamp
│
├── joinRequests/
│   └── {requestId}/
│       ├── requestId: string
│       ├── adId: string
│       ├── requestorUid: string
│       ├── requestorName: string
│       ├── adCreatorUid: string
│       ├── status: "PENDING" | "ACCEPTED" | "REJECTED"
│       ├── chatRoomId: string (set on accept)
│       └── timestamp: long
│
└── chats/
    └── {adId}_{uid1}_{uid2}/    ← deterministic ID
        └── {messageId}/
            ├── messageId: string
            ├── senderId: string
            ├── senderName: string
            ├── text: string
            ├── type: "text" | "system"
            ├── timestamp: long
            └── read: boolean
```

---

## 10. Deploying to Google Play Store

### Step 1 — Generate a Signed Release APK

In Android Studio:
```
Build → Generate Signed Bundle / APK
  → APK → Next
  → Create new keystore (SAVE THIS FILE SAFELY — you can never change it)
    · Key alias: safar_key
    · Validity: 25 years
  → Build Type: release
  → Finish
```

Output: `app/release/app-release.apk`

### Step 2 — Play Console Setup
1. Go to https://play.google.com/console
2. Create developer account ($25 one-time fee)
3. **Create app** → App name: SAFAR → Default language: English
4. Fill in:
   - **App description** (what it does, safety focus)
   - **Screenshots** (phone + 7-inch tablet)
   - **App icon** (512×512 PNG)
   - **Feature graphic** (1024×500 PNG)

### Step 3 — Content Rating & Policy
- Content rating questionnaire: Select "Social" category
- Target audience: 18+
- Data safety: Declare phone number collection + Firebase usage

### Step 4 — Upload & Publish
- Production → Create new release → Upload APK
- Add release notes
- Submit for review (~1-3 days)

### Pre-launch Checklist:
- [ ] `google-services.json` has production Firebase project
- [ ] Maps API key restricted to release SHA-1 + package name
- [ ] Firebase Database rules deployed (not test mode)
- [ ] `minifyEnabled true` in `build.gradle` for release
- [ ] App icon in all mipmap densities (mdpi/hdpi/xhdpi/xxhdpi/xxxhdpi)
- [ ] Privacy Policy URL added to Play Console

---

## 11. Scaling Roadmap

### Phase 2 — Trust & Safety
```java
// Add to User model:
private double rating;
private int ratingCount;
private boolean isVerified;

// New Firebase node: /reports/{reportId}
// New Activity: RateUserActivity (post-trip)
```

### Phase 3 — Search Filters
```java
// Add filter fields to RideAd:
private boolean ladiesOnly;
private boolean nonSmoking;
private boolean acAvailable;

// Add filter UI to ListFragment with chip group
```

### Phase 4 — Google Places Autocomplete
Replace manual city input with Places API autocomplete:
```gradle
implementation 'com.google.android.libraries.places:places:3.3.0'
```
```java
// In PostAdActivity, replace EditText with:
AutocompleteSupportFragment autocompleteFragment =
    (AutocompleteSupportFragment) getSupportFragmentManager()
        .findFragmentById(R.id.autocomplete_fragment);
autocompleteFragment.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG));
```
This gives exact GPS coordinates, replacing the fallback city→coords mapping.

### Phase 5 — Backend Migration (High Traffic)
When Firebase costs become significant:
- Migrate to **Node.js + Express** backend
- Use **PostgreSQL + PostGIS** for spatial queries
  ```sql
  SELECT * FROM ride_ads
  WHERE ST_DWithin(origin_point, ST_MakePoint(lng, lat)::geography, 50000)
    AND departure_time > NOW()
  ORDER BY departure_time ASC;
  ```
- Use **Redis** for active ride caching
- Keep Firebase only for real-time chat

### Phase 6 — In-App Payments (Optional)
- Integrate **JazzCash / EasyPaisa SDK** for Pakistan
- Or Stripe for international cards
- Escrow model: payment held until trip confirmed complete

---

## 12. Troubleshooting

| Error | Fix |
|---|---|
| `google-services.json` not found | Download from Firebase Console → Project Settings → Your Apps |
| `Maps API key invalid` | Enable Maps SDK for Android in Google Cloud Console |
| Phone auth not working | Ensure SHA-1 fingerprint added to Firebase Console → Project Settings → Your Android App |
| Firebase permission denied | Check Database Rules — ensure user is authenticated |
| OTP not received | Firebase Phone Auth requires a real device (not emulator) for production; use test numbers in Firebase Console for emulator testing |
| Gradle sync fails | File → Invalidate Caches → Restart; or check internet connection |
| `ClassNotFoundException` on model | Check `proguard-rules.pro` keeps `com.safar.app.models.**` |
| Map not showing | Verify `YOUR_GOOGLE_MAPS_API_KEY` in `AndroidManifest.xml` is replaced |
| App crashes on launch | Check Logcat in Android Studio; most likely `google-services.json` is still the placeholder |

### Firebase Test Phone Numbers (for emulator/testing)
In Firebase Console → Authentication → Sign-in method → Phone → Scroll to "Phone numbers for testing":
```
+92 300 0000000  →  OTP: 123456
+92 300 1111111  →  OTP: 654321
```

---

## License
MIT — Free for personal and commercial use.

---

*Built for the SAFAR Community Carpool Platform. Pakistan 🇵🇰*
