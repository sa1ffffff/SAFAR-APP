# SAFAR — Project Context
> **Copilot: Read this entire file before suggesting any code.**
> Every suggestion must align with the architecture, language rules, naming conventions, UI standards, and Firebase schema defined here.
> Cross-reference with `.github/copilot-instructions.md` for the enforcement rules.

---

## 1. Project Identity

| Field        | Value                                                        |
|--------------|--------------------------------------------------------------|
| App Name     | SAFAR (سفر — Urdu for "Journey")                             |
| Type         | Community Carpool Marketplace                                |
| Platform     | Android only — no web app, no separate backend server        |
| Backend      | Firebase as BaaS (Auth, Firestore, Realtime DB, FCM)         |
| Architecture | MVP — strict separation: Models, Views, Presenters, Repos    |
| UI Standard  | Material Design 3 · Dark Theme · Yellow `#FFD600` / Black    |
| Inspiration  | Yango ride-hailing aesthetic                                 |
| Context      | BSCS Final Year Project · SEECS, NUST · Pakistan             |

---

## 2. Language Rules (Enforced)

| Layer                                      | Language         |
|--------------------------------------------|------------------|
| Models, Repositories, Presenters, Services | **Java only**    |
| Activities, Fragments, Adapters            | **Java or Kotlin — match the existing file** |
| XML layouts, drawables, resources          | XML              |

- Never mix Java and Kotlin inside the same file
- Never migrate a file from one language to the other
- Never suggest Kotlin for any logic layer

---

## 3. Architecture — MVP (Strict)

```
Model       →  Plain Java POJOs. No Android imports. No Firebase calls.
Repository  →  All Firebase read/write operations. Returns data via callbacks.
Presenter   →  All business logic. Calls Repository. Updates View via contract.
View        →  Activity or Fragment. Only UI updates. Zero logic. Zero Firebase.
```

**Hard rules:**
- Business logic lives in Presenters only
- Firebase calls live in Repositories only
- Activities/Fragments implement the View contract interface and nothing else
- Always null-check `view` in Presenter before calling any view method
- Every Presenter has an `onDestroy()` that sets `view = null`

---

## 4. Package Structure

```
com.safar.app/
├── models/
│   ├── User.java
│   ├── Ride.java
│   ├── Message.java
│   ├── JoinRequest.java
│   └── Report.java
├── repositories/
│   ├── UserRepository.java
│   ├── RideRepository.java
│   ├── ChatRepository.java
│   └── RequestRepository.java
├── presenters/
│   ├── AuthPresenter.java
│   ├── HomePresenter.java
│   ├── PostRidePresenter.java
│   ├── RideDetailPresenter.java
│   ├── ChatPresenter.java
│   ├── ProfilePresenter.java
│   ├── RequestPresenter.java
│   └── ReportPresenter.java
├── views/
│   ├── contracts/              ← MVP View + Presenter interfaces
│   ├── activities/
│   └── fragments/
├── adapters/                   ← RecyclerView adapters only
├── services/
│   └── SafarFCMService.java    ← extends FirebaseMessagingService
└── utils/                      ← Helpers, constants, map utils
```

---

## 5. Naming Conventions (Never Deviate)

| Component          | Pattern                        | Example                     |
|--------------------|--------------------------------|-----------------------------|
| Activity           | `<Feature>Activity`            | `RideDetailActivity`        |
| Fragment           | `<Feature>Fragment`            | `HomeFragment`              |
| Presenter          | `<Feature>Presenter`           | `ChatPresenter`             |
| View Contract      | `<Feature>Contract.View`       | `HomeContract.View`         |
| Presenter Contract | `<Feature>Contract.Presenter`  | `HomeContract.Presenter`    |
| Repository         | `<Feature>Repository`          | `RideRepository`            |
| Adapter            | `<Feature>Adapter`             | `RideListAdapter`           |
| Activity Layout    | `activity_<name>.xml`          | `activity_ride_detail.xml`  |
| Fragment Layout    | `fragment_<name>.xml`          | `fragment_home.xml`         |
| Item Layout        | `item_<name>.xml`              | `item_ride_card.xml`        |
| Drawable           | `ic_<name>.xml` / `bg_<name>.xml` | `ic_pin_yellow.xml`      |

---

## 6. MVP Contract Pattern (Always Use This Exact Structure)

```java
// views/contracts/RideDetailContract.java
public interface RideDetailContract {

    interface View {
        void showRideDetails(Ride ride);
        void showError(String message);
        void showLoading(boolean show);
        void navigateToChat(String rideId);
    }

    interface Presenter {
        void loadRide(String rideId);
        void sendJoinRequest(String rideId, int seats);
        void onDestroy();
    }
}
```

```java
// presenters/RideDetailPresenter.java
public class RideDetailPresenter implements RideDetailContract.Presenter {
    private RideDetailContract.View view;
    private final RideRepository rideRepo;

    public RideDetailPresenter(RideDetailContract.View view) {
        this.view = view;
        this.rideRepo = new RideRepository();
    }

    @Override
    public void loadRide(String rideId) {
        if (view != null) view.showLoading(true);
        rideRepo.getRide(rideId, ride -> {
            if (view == null) return;
            view.showLoading(false);
            view.showRideDetails(ride);
        });
    }

    @Override
    public void onDestroy() { view = null; }
}
```

---

## 7. Data Models (Do Not Change Field Names)

### User.java
```java
String uid;
String name;
String phone;
String profilePicUrl;
float rating;
int ridesGiven;
int ridesTaken;
long createdAt;
```

### Ride.java
```java
String rideId;
String driverId;
String origin;
String destination;
GeoPoint originGeo;
GeoPoint destGeo;
int totalSeats;
int seatsLeft;
double pricePerSeat;        // PKR
long departureTime;         // epoch ms
String status;              // ACTIVE | FULL | COMPLETED | CANCELLED
long createdAt;
```

### Message.java
```java
String msgId;
String rideId;
String senderId;
String senderName;
String text;
long timestamp;
```

### JoinRequest.java
```java
String reqId;
String rideId;
String riderId;
String driverId;
int seatsRequested;
String status;              // PENDING | ACCEPTED | REJECTED
long createdAt;
```

### Report.java
```java
String reportId;
String reporterId;
String targetUserId;
String targetRideId;        // nullable
String reason;
long timestamp;
```

---

## 8. Firebase Schema (Never Modify)

### Firestore Collections
```
/users/{uid}                ← User fields
/rides/{rideId}             ← Ride fields
/requests/{reqId}           ← JoinRequest fields
/reports/{reportId}         ← Report fields
/ratings/{ratingId}         ← fromUid, toUid, rideId, stars(1–5), comment
```

### Firebase Realtime Database — Chat Only
```
/chats/{rideId}/messages/{msgId}/
    ├── senderId
    ├── senderName
    ├── text
    └── timestamp
```

### FCM
- Device token stored as `fcmToken: String` inside `/users/{uid}`
- Never change this field name

---

## 9. Repository Pattern (Always Follow This)

```java
public class RideRepository {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private static final String COLLECTION = "rides";

    public void postRide(Ride ride, OnSuccessListener<Void> onSuccess,
                         OnFailureListener onFailure) {
        db.collection(COLLECTION)
          .document(ride.getRideId())
          .set(ride)
          .addOnSuccessListener(onSuccess)
          .addOnFailureListener(onFailure);
    }

    public void getRide(String rideId, OnRideFetchedListener listener) {
        db.collection(COLLECTION).document(rideId)
          .get()
          .addOnSuccessListener(snap -> listener.onFetched(snap.toObject(Ride.class)));
    }
}
```

---

## 10. UI & Frontend Standards (Enforced)

- **Framework**: Material Design 3 (MDC-Android) only — never AppCompat widgets
- **Layout**: ConstraintLayout for all screens — no overlapping views, no clipping
- **Buttons**: Every button must have normal, pressed, and disabled states — no ghost buttons, no invisible tap targets, no placeholder buttons
- **Feedback**: Every clickable element must give immediate feedback — ripple effect, loading indicator, or visible state change
- **Transitions**: Use Material Motion — fade-through between screens, shared element transitions where applicable, slide for lateral navigation
- **Animations**: Enter/exit on Activities and Fragments, subtle item animations on RecyclerViews — no jarring or excessive effects
- **Images**: Glide only — never load images on the main thread
- **No useless UI**: Never generate buttons, icons, or views that have no connected action

### Color System
```xml
<color name="yellow">#FFD600</color>          <!-- Primary accent -->
<color name="yellow_dark">#C9A800</color>     <!-- Pressed / dark variant -->
<color name="bg_dark">#0A0A0A</color>         <!-- Window background -->
<color name="surface">#111111</color>          <!-- Card surfaces -->
<color name="surface_2">#181818</color>        <!-- Elevated surfaces -->
<color name="text_primary">#F0F0F0</color>     <!-- Body text -->
<color name="text_muted">#6B6B6B</color>       <!-- Hints, captions -->
<color name="green">#22C55E</color>            <!-- Success states -->
<color name="red">#EF4444</color>              <!-- Error states -->
```

### Maps
- Style: Custom dark JSON at `res/raw/map_style_dark.json`
- Markers: Yellow pill-shaped drawable at `res/drawable/ic_pin_yellow.xml`

---

## 11. Feature Status

### Built ✅ — Do Not Rebuild or Modify Unless Explicitly Asked
| Feature                    | Notes                                        |
|----------------------------|----------------------------------------------|
| Phone OTP Authentication   | Firebase Auth, full flow                     |
| User Profile               | View & edit, Glide for profile picture       |
| Post Ride Ad               | Origin, dest, seats, price, departure time   |
| Map-Centric Ride Discovery | Dark map style, yellow pill pins             |
| Join Requests              | Rider sends → Driver accepts / rejects       |
| Transactional Seat Booking | Firestore transaction on `seatsLeft`         |
| Real-Time Chat             | Per-ride chat via Realtime Database          |
| Push Notifications         | FCM foreground + background                  |
| Safety Onboarding          | First-launch screen                          |
| Reporting System           | Report user/ride → Firestore                 |
| Rating Data Model          | Firestore schema ready                       |

### Not Built ❌ — Do Not Assume These Exist
| Feature                    | Notes                                        |
|----------------------------|----------------------------------------------|
| Rating / Review UI         | Model exists — Activity and wiring missing   |
| Google Places Autocomplete | Plain dialog stub — Places SDK not wired     |
| Unit Tests                 | Zero tests — JUnit + Mockito needed          |
| Admin / Moderation Panel   | Not started                                  |
| Project Report             | Not started                                  |

---

## 12. Dos and Don'ts

| ✅ DO                                              | ❌ DON'T                                        |
|----------------------------------------------------|-------------------------------------------------|
| Use Java for all logic layers                      | Suggest Kotlin for models, repos, or presenters |
| Match language of the existing UI file             | Migrate files between Java and Kotlin           |
| Keep all logic in Presenters                       | Put any logic in Activities or Fragments        |
| Use Firestore for persistent data                  | Use Room, SQLite, or SharedPreferences for data |
| Use Realtime DB only for chat                      | Use Realtime DB for rides or users              |
| Use Glide for all image loading                    | Load images directly or use Picasso / Coil      |
| Follow naming conventions above exactly            | Rename existing classes, methods, or XML files  |
| Use MDC-Android M3 components                      | Use deprecated AppCompat widgets                |
| Give every button a visible state                  | Create ghost, invisible, or placeholder buttons |
| Use Material Motion transitions                    | Use abrupt or janky custom transitions          |
| Null-check view in Presenter before every call     | Call view methods without null check            |
| Generate complete, working code only               | Generate TODO stubs or placeholder code         |
| Ask before adding a Gradle dependency              | Add dependencies silently                       |
| Never touch Firebase schema, Auth, or DB structure | Modify collection names or document fields      |

---

*SAFAR · BSCS FYP · SEECS NUST · Updated March 2026*
