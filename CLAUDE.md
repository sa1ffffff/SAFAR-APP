# Safar — notes for coding agents

Kotlin + Jetpack Compose, MVVM, single activity. Firebase Realtime Database (not Firestore).

## Architecture
- `data/` owns all Firebase access. Screens and ViewModels never import `com.google.firebase.database`.
- Models in `data/model/Models.kt` map to and from `DataSnapshot` by hand (`from()` / `toMap()`). Keep database key names stable; old app data depends on them.
- Each feature is one file in `ui/<feature>/` holding its ViewModel (extends `SafarViewModel`) and screen.
- ViewModels are created with `appViewModel { Container -> VM }` and get repositories from `AppContainer`.
- Use `action { }` in ViewModels for writes; it turns exceptions into snackbar messages.
- The current user comes from `app.session` (`user`, `requireUser()`).

## Database
- Every new node needs rules in `database.rules.json`, deployed with `firebase deploy --only database`.
- `/conversations` cannot be listed. Use the `/userConversations/{uid}` index.
- Ratings are aggregated in `/ratings/{uid}` with a transaction, never on `/users`.
- Push tokens live in `/pushTokens/{uid}` (owner-only), not on the user record.

## UI
- Colors and type come only from `Safar.colors` / `Safar.type`: a monochrome palette with color only for meaning (positive, negative, info).
- Floating controls use `Glass`/`GlassIconButton`. Glass blurs only when a `HazeState` is provided; over Google Maps it's tinted translucency.
- Motion: 150–300 ms, ease-out, subtle press scale (0.97). No bouncy springs except the review stars.
- Secondary screens use `ScreenScaffold`.

## Secrets
- `MAPS_API_KEY` goes in `local.properties`. Release signing goes in `keystore.properties`. Both are gitignored.

## Build
`./gradlew :app:assembleDebug`
