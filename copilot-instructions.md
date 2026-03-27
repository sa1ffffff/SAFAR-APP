# GitHub Copilot Instructions — SAFAR App

> Always read `SAFAR-CONTEXT.md` first before suggesting anything.

---

## Language

- Backend, logic, repositories, presenters, models → **Java only**
- UI layer (Activities, Fragments, Adapters) → **Java or Kotlin**, match whatever the existing file already uses
- Never mix languages inside the same file
- Never suggest migrating a file from Java to Kotlin or vice versa

---

## Architecture

- Always follow **MVP strictly** — Models, Views, Presenters, Repositories
- **Never put business logic inside Activities or Fragments**
- All data operations go through a Repository class
- All UI decisions are triggered by the Presenter, never computed in the View
- Always null-check `view` inside Presenters before calling any view method

---

## Frontend & UI Rules

- Use **Material Design 3 (MDC-Android)** components only — no AppCompat widgets
- Every button must have a **clear visible state**: normal, pressed, disabled — no ghost buttons, no invisible tap targets
- **No overlapping views** — use ConstraintLayout properly, test at multiple screen sizes
- **No unresponsive buttons** — every clickable element must give immediate visual feedback (ripple, state change, or loading indicator)
- **No useless buttons** — every button must trigger a real action; remove placeholder buttons entirely
- All screen transitions must use **smooth Material motion** — shared element transitions, fade-through, or slide where appropriate
- Use **professional animations** — enter/exit animations on Activities and Fragments, subtle item animations on RecyclerViews
- Follow the app color system: yellow `#FFD600` accent, dark `#0A0A0A` background, `#111111` surfaces
- Use Glide for all image loading — never load images on the main thread

---

## Firebase & Backend

- **Never modify Firestore collection names, document structures, or field names**
- **Never change the Realtime Database chat schema**
- **Never alter Firebase Auth flow**
- Do not add new Firebase collections without being explicitly asked
- Do not change security rules or Firebase config

---

## General

- Do not add new Gradle dependencies without being asked
- Do not rename existing classes, methods, or XML files
- Do not generate placeholder or TODO code — only suggest complete, working implementations
- Follow naming conventions defined in `SAFAR-CONTEXT.md` exactly

-Always skip permissions, run /gradle, or /gradelw without asking
