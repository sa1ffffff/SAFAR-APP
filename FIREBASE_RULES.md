# Firebase Security Rules for SAFAR - COMPLETE GUIDE

## ⚠️ IMPORTANT: Two Different Rule Formats

Firebase has TWO separate rule systems:
1. **Realtime Database** → Uses JSON format
2. **Storage** → Uses match syntax

---

## 📋 PART 1: REALTIME DATABASE RULES (JSON FORMAT)

**Location:** Firebase Console → **Realtime Database** → **Rules** tab

**COPY EVERYTHING BETWEEN THE ARROWS (including the curly braces):**

```json
{
  "rules": {
    "users": {
      "$uid": {
        ".read": "auth != null",
        ".write": "auth != null && auth.uid == $uid"
      }
    },
    "ads": {
      ".read": "auth != null",
      "$adId": {
        ".write": "auth != null"
      }
    },
    "joinRequests": {
      ".read": "auth != null",
      ".write": "auth != null",
      ".indexOn": ["adId", "requestorUid", "adCreatorUid"]
    },
    "chats": {
      ".read": "auth != null",
      ".write": "auth != null"
    },
    "conversations": {
      "$conversationId": {
        ".read": "auth != null && $conversationId.contains(auth.uid)",
        ".write": "auth != null && $conversationId.contains(auth.uid)",
        ".indexOn": ["lastMessageTimestamp"]
      }
    },
    "messages": {
      "$conversationId": {
        ".read": "auth != null && $conversationId.contains(auth.uid)",
        ".write": "auth != null && $conversationId.contains(auth.uid)",
        "$messageId": {
          ".validate": "newData.child('senderId').val() == auth.uid"
        },
        ".indexOn": ["timestamp"]
      }
    }
  }
}
```

**Steps:**
1. Go to Firebase Console → **Realtime Database** → **Rules** tab
2. Delete ALL existing content
3. Paste the JSON above (starting from { to })
4. Click **Publish**

---

## 📦 PART 2: STORAGE RULES (MATCH SYNTAX)

**Location:** Firebase Console → **Storage** → **Rules** tab

**COPY EVERYTHING BELOW:**

```
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    
    match /profile_pictures/{allFiles=**} {
      allow read: if true;
      allow write: if request.auth != null
                   && request.resource.size < 5 * 1024 * 1024
                   && request.resource.contentType.matches('image/.*');
    }
    
  }
}
```

**Steps:**
1. Go to Firebase Console → **Storage** → **Rules** tab  
2. Delete ALL existing content
3. Paste the rules above (starting from rules_version)
4. Click **Publish**

---

## ✅ Verification Checklist

After publishing both:
- [ ] Realtime Database rules published (JSON format)
- [ ] Storage rules published (match format)
- [ ] Waited 30-60 seconds
- [ ] Restarted the app

---

## 🔍 Troubleshooting

**Error: "user does not have permission to access this object"**
1. Make sure you're logged in to the app
2. Go to Firebase Console → **Storage** → **Rules** tab
3. Copy and paste the Storage rules exactly as shown above
4. Click **Publish** and wait 30-60 seconds
5. Close and restart the app
6. Try uploading again

**Error: Missing match keyword**
→ You pasted Realtime Database rules into Storage (or vice versa)

**Error: Unexpected {**
→ Make sure you copied the ENTIRE rule including the first { and last }

**Still getting Permission Denied?**
1. Clear app data: Settings → Apps → SAFAR → Clear Data
2. Re-login to the app
3. Check you are logged in: Firebase Console → Authentication → Users
4. Check file size is under 5MB
5. Make sure you're uploading an image file (jpg, png, etc.)
