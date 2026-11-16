# SAFAR FIREBASE SECURITY RULES - REVIEWS MODULE
# Add these rules to your Firebase Realtime Database Rules

{
  "rules": {
    "reviews": {
      ".read": true,  // Anyone can read reviews (public ratings)
      "$reviewId": {
        ".write": "auth != null && (!data.exists() || data.child('reviewerId').val() === auth.uid)",
        ".validate": "newData.hasChildren(['reviewId', 'rideAdId', 'reviewerId', 'revieweeId', 'rating', 'timestamp'])",
        "reviewId": {
          ".validate": "newData.isString() && newData.val() === $reviewId"
        },
        "rideAdId": {
          ".validate": "newData.isString()"
        },
        "reviewerId": {
          ".validate": "newData.isString() && newData.val() === auth.uid"
        },
        "revieweeId": {
          ".validate": "newData.isString() && newData.val() != auth.uid"
        },
        "reviewerName": {
          ".validate": "newData.isString()"
        },
        "rating": {
          ".validate": "newData.isNumber() && newData.val() >= 1 && newData.val() <= 5"
        },
        "comment": {
          ".validate": "newData.isString() && newData.val().length <= 500"
        },
        "timestamp": {
          ".validate": "newData.isNumber()"
        },
        "rideCompletedAt": {
          ".validate": "newData.isNumber()"
        }
      }
    },
    
    "joinRequests": {
      "$requestId": {
        // Allow reading if user is part of the request
        ".read": "auth != null && (data.child('requestorUid').val() === auth.uid || data.child('adCreatorUid').val() === auth.uid)",
        
        // Allow writing if user is part of the request
        ".write": "auth != null && (data.child('requestorUid').val() === auth.uid || data.child('adCreatorUid').val() === auth.uid || !data.exists())",
        
        // Completion flags can only be set by respective parties
        "requesterMarkedComplete": {
          ".write": "auth != null && data.parent().child('requestorUid').val() === auth.uid"
        },
        "creatorMarkedComplete": {
          ".write": "auth != null && data.parent().child('adCreatorUid').val() === auth.uid"
        },
        
        // Review tracking flags
        "requesterReviewedCreator": {
          ".validate": "newData.isBoolean()"
        },
        "creatorReviewedRequester": {
          ".validate": "newData.isBoolean()"
        },
        
        // Completion status
        "rideCompleted": {
          ".validate": "newData.isBoolean()"
        },
        "completedAt": {
          ".validate": "newData.isNumber()"
        }
      }
    },
    
    "users": {
      "$uid": {
        ".read": true,  // Public profiles
        ".write": "auth != null && auth.uid === $uid",
        
        // Rating fields - can be updated by system
        "rating": {
          ".validate": "newData.isNumber() && newData.val() >= 0 && newData.val() <= 5"
        },
        "ratingCount": {
          ".validate": "newData.isNumber() && newData.val() >= 0"
        }
      }
    }
  }
}
