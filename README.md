# SAFAR — Community Carpool Marketplace 🚗

A peer-to-peer ride-sharing platform built for Pakistan, connecting drivers with empty seats to passengers heading the same way.

---

## 📌 Overview

SAFAR is an Android application that allows users to:
- Post ride offers (e.g., Lahore → Islamabad)
- Discover available rides via map or list view
- Request to join rides
- Chat with drivers/passengers after approval

The app focuses on simplicity, real-time interaction, and community-driven transport — no in-app payments, just coordination.

---

## 🛠 Tech Stack

- **Language:** Java  
- **Architecture:** MVP (Model-View-Presenter)  
- **UI:** XML + Material Design 3  
- **Backend:** Firebase Realtime Database  
- **Authentication:** Firebase Phone Auth (OTP)  
- **Maps:** Google Maps SDK  
- **Notifications:** Firebase Cloud Messaging (FCM)  

---

## 🧱 Architecture (MVP)

- **Model:** Data classes (`RideAd`, `User`, `ChatMessage`, `JoinRequest`)
- **View:** Activities & Fragments (UI layer)
- **Presenter:** Handles logic, Firebase operations, and communication between Model & View

---

## 📂 Project Structure
com.safar.app/
├── models/ # Data classes
├── presenters/ # Business logic
├── views/
│ ├── activities/ # Screens
│ ├── fragments/ # UI components (Map, List, Inbox)
│ └── adapters/ # RecyclerView adapters
├── firebase/ # Firebase manager & messaging
└── utils/ # Helper classes

📱 Features
🔐 Authentication
Google Login

Phone number login with OTP

🗺 Ride Discovery

Map view with ride pins

List view with search

🚗 Ride Posting

Add origin, destination, time, seats

📩 Requests System

Send join requests

Accept or reject requests

💬 Real-Time Chat

Chat unlocked after request acceptance

Firebase-powered instant messaging
---

## ⚙️ Setup

### 1. Firebase
- Create a Firebase project
- Add Android app (`com.safar.app`)
- Download and replace `google-services.json`
- Enable:
  - Phone Authentication
  - Realtime Database
  - Cloud Messaging


