# Safar

Community carpooling for Pakistan. Drivers post rides with empty seats; riders heading the same way request a seat, chat, and split the fuel. No in-app payments.

## Features

- Sign in with Google, plus a phone number so riders and drivers can call or WhatsApp each other
- Map of open rides with seat markers, nearest first
- Post a ride: pickup/drop-off on the map, date and time, seats, optional price per seat, or repeat it weekly on chosen days
- Driving routes on the map, with distance and time
- Drivers start the ride when they set off; rides close an hour after leaving
- Seat requests with accept/decline; seats are reserved in a transaction so rides can't be overbooked
- One-to-one chat
- Mark rides complete, then rate each other
- In-app notifications and system push for ride updates and chat messages
- Reporting from profiles and rides
- Light and dark themes following the system, with a manual override

## Stack

| | |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose, Material 3, Haze (backdrop blur) |
| Architecture | MVVM, single activity, Navigation Compose |
| Backend | Firebase Auth, Realtime Database, Cloud Messaging; a Cloudflare Worker (`cloudflare/`) for photos, push delivery and daily cleanup |
| Maps | Google Maps SDK + maps-compose, Android Geocoder |

## Project layout

```
app/src/main/java/com/safar/app/
├── SafarApp.kt              App class + AppContainer (manual dependency injection)
├── MainActivity.kt          The only activity
├── SafarMessagingService.kt FCM
├── data/
│   ├── model/Models.kt      Domain models, mapped to/from Realtime Database by hand
│   ├── Repositories.kt      All Firebase access (auth, users, rides, requests, chat, reviews, notifications, reports)
│   ├── SafarApi.kt          Client for the Cloudflare Worker
│   ├── Session.kt           Signed-in user + start gate shared by every screen
│   ├── FirebaseFlows.kt     Listener → Flow and transaction helpers
│   └── Preferences.kt       Device-local settings
└── ui/
    ├── Navigation.kt        Routes and nav graphs
    ├── Base.kt              SafarViewModel, error messages, ViewModel factory
    ├── theme/               Colors, type, shapes
    ├── components/          Buttons, fields, cards, glass surfaces, scaffold
    └── <feature>/           One file per feature: ViewModel + screen
```

Rules of the road for contributors:
- Screens never touch Firebase. They call a ViewModel, which calls a repository.
- Repositories expose `Flow`s for live data and `suspend` functions for writes.
- New database nodes need a matching entry in `database.rules.json`.

## Running it

See [SETUP.md](SETUP.md) for the Firebase and Google Cloud steps. Then:

```bash
./gradlew :app:assembleDebug
```

Designs live in Figma: `Safar — App Revamp`.

Earlier documentation (v1, Java/MVP) is kept in `docs/archive/`.
