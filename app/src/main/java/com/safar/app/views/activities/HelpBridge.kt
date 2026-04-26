package com.safar.app.views.activities

import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import com.safar.app.views.ui.screens.HelpScreen
import com.safar.app.views.ui.theme.SafarTheme

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// HELP & FAQ BRIDGE
// Connects Java HelpActivity to Jetpack Compose HelpScreen
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

/**
 * FAQ item data model.
 */
data class FaqItem(
    val question: String,
    val answer: String,
    val category: FaqCategory
)

/**
 * FAQ categories for organization.
 */
enum class FaqCategory(val displayName: String) {
    GETTING_STARTED("Getting Started"),
    POSTING_RIDES("Posting Rides"),
    FINDING_RIDES("Finding Rides"),
    REQUESTS("Requests & Booking"),
    CHAT("Chat & Communication"),
    SAFETY("Safety & Trust"),
    PAYMENTS("Payments"),
    ACCOUNT("Account & Profile"),
    TROUBLESHOOTING("Troubleshooting")
}

/**
 * FAQ database for SAFAR app.
 */
object FaqDatabase {
    val ALL_FAQS = listOf(
        // ─── GETTING STARTED ────────────────────────────────────────────
        FaqItem(
            question = "What is SAFAR?",
            answer = "SAFAR (سفر) is a community-based carpool marketplace for Pakistan. It connects drivers with empty seats to passengers heading the same way, making travel more affordable, sustainable, and social.",
            category = FaqCategory.GETTING_STARTED
        ),
        FaqItem(
            question = "How does SAFAR work?",
            answer = "SAFAR has two user types:\n\n• Drivers: Post available seats on upcoming trips\n• Passengers: Browse rides on map or list view, send join requests\n\nOnce a request is accepted, both parties can chat to coordinate pickup/drop-off details. SAFAR handles coordination — payment happens offline between riders.",
            category = FaqCategory.GETTING_STARTED
        ),
        FaqItem(
            question = "Is SAFAR free to use?",
            answer = "Yes! SAFAR is completely free. There are no listing fees, booking fees, or commissions. Drivers set their own price per seat, and payment is arranged directly between riders.",
            category = FaqCategory.GETTING_STARTED
        ),
        FaqItem(
            question = "Do I need to verify my account?",
            answer = "Yes. All users must verify their phone number via OTP during signup. This ensures a trusted community and helps prevent fake accounts.",
            category = FaqCategory.GETTING_STARTED
        ),

        // ─── POSTING RIDES ──────────────────────────────────────────────
        FaqItem(
            question = "How do I post a ride?",
            answer = "1. Tap the '+' button on the home screen\n2. Select origin and destination cities\n3. Set departure date and time\n4. Enter number of available seats\n5. Set price per seat (optional)\n6. Tap 'Post Ride'\n\nYour ride will appear on the map for nearby passengers to discover.",
            category = FaqCategory.POSTING_RIDES
        ),
        FaqItem(
            question = "Can I edit a ride after posting?",
            answer = "Currently, you cannot edit a posted ride. If you need to make changes, delete the original ride from 'My Ads' and post a new one with updated details.",
            category = FaqCategory.POSTING_RIDES
        ),
        FaqItem(
            question = "How do I cancel a posted ride?",
            answer = "1. Go to your Profile\n2. Tap 'My Ads'\n3. Find the ride you want to cancel\n4. Tap the ride and select 'Delete'\n\nNote: If passengers have already been accepted, please inform them via chat before canceling.",
            category = FaqCategory.POSTING_RIDES
        ),
        FaqItem(
            question = "What should I set as the price?",
            answer = "Prices are optional but recommended. Consider:\n\n• Fuel cost divided by total seats\n• Toll charges (if applicable)\n• Vehicle wear and tear\n\nMost rides in Pakistan range from ₨50-200 per seat for intercity trips. Keep it fair and affordable.",
            category = FaqCategory.POSTING_RIDES
        ),

        // ─── FINDING RIDES ──────────────────────────────────────────────
        FaqItem(
            question = "How do I find a ride?",
            answer = "SAFAR offers two discovery modes:\n\n• Map View: Browse rides shown as pins on the map\n• List View: Scroll through rides sorted by departure time\n\nTap any ride to see full details including driver profile, price, and available seats.",
            category = FaqCategory.FINDING_RIDES
        ),
        FaqItem(
            question = "Can I filter rides?",
            answer = "Currently, rides are shown based on your location. You can switch between Map and List views using the tabs on the home screen. Advanced filters (by price, departure time, etc.) are coming soon.",
            category = FaqCategory.FINDING_RIDES
        ),
        FaqItem(
            question = "What information can I see about the driver?",
            answer = "When viewing a ride, you can see:\n\n• Driver's name and profile photo\n• Phone verification status\n• Number of rides given (experience)\n• Average rating from past passengers\n• Vehicle details (if provided)",
            category = FaqCategory.FINDING_RIDES
        ),

        // ─── REQUESTS & BOOKING ─────────────────────────────────────────
        FaqItem(
            question = "How do I request to join a ride?",
            answer = "1. Find a ride that matches your route\n2. Tap on the ride to open details\n3. Select number of seats you need\n4. Tap 'Send Request'\n\nThe driver will receive your request and can accept or reject it.",
            category = FaqCategory.REQUESTS
        ),
        FaqItem(
            question = "What happens after I send a request?",
            answer = "Your request goes to the driver's Inbox. They can:\n\n• Accept: You'll receive a notification and chat will be unlocked\n• Reject: You'll be notified that the ride isn't available\n\nYou can track request status in your Inbox.",
            category = FaqCategory.REQUESTS
        ),
        FaqItem(
            question = "Can I cancel a join request?",
            answer = "Yes. If the driver hasn't responded yet, you can cancel your request from the Inbox. Once accepted, please use the chat to inform the driver if you need to cancel.",
            category = FaqCategory.REQUESTS
        ),
        FaqItem(
            question = "How do I accept/reject requests as a driver?",
            answer = "1. Open your Inbox\n2. Tap on the request notification\n3. Review the passenger's profile\n4. Tap 'Accept' or 'Reject'\n\nAccepting opens a chat thread with the passenger.",
            category = FaqCategory.REQUESTS
        ),
        FaqItem(
            question = "What if the ride fills up?",
            answer = "When all seats are booked, the ride automatically changes to 'FULL' status and stops appearing in search results. Accepted passengers can still access the chat.",
            category = FaqCategory.REQUESTS
        ),

        // ─── CHAT & COMMUNICATION ───────────────────────────────────────
        FaqItem(
            question = "When can I chat with the driver/passenger?",
            answer = "Chat is only available after the driver accepts your join request. This ensures privacy and prevents spam.",
            category = FaqCategory.CHAT
        ),
        FaqItem(
            question = "What should I discuss in chat?",
            answer = "Use chat to coordinate:\n\n• Exact pickup location\n• Pickup time\n• Landmark or meeting point\n• Payment method (cash on ride)\n• Any special requirements (luggage, stops, etc.)\n\nNever share sensitive personal information or financial details.",
            category = FaqCategory.CHAT
        ),
        FaqItem(
            question = "Can I share my location in chat?",
            answer = "Not yet. For now, describe your pickup location clearly using landmarks. Location sharing is planned for a future update.",
            category = FaqCategory.CHAT
        ),

        // ─── SAFETY & TRUST ─────────────────────────────────────────────
        FaqItem(
            question = "Is SAFAR safe?",
            answer = "SAFAR takes safety seriously:\n\n• All users verified via phone number\n• Driver/passenger ratings visible\n• In-app reporting system\n• Safety guidelines provided\n\nHowever, always trust your instincts. Share trip details with someone you trust and meet in public places when possible.",
            category = FaqCategory.SAFETY
        ),
        FaqItem(
            question = "How do I report a user or ride?",
            answer = "If you experience unsafe behavior:\n\n1. Tap the ride or user profile\n2. Tap the 'Report' button (⚠️)\n3. Select a reason (harassment, fake profile, unsafe driving, etc.)\n4. Submit\n\nOur team reviews all reports within 24 hours. Serious violations result in account suspension.",
            category = FaqCategory.SAFETY
        ),
        FaqItem(
            question = "Can I see ratings before joining a ride?",
            answer = "Yes. Each user profile shows:\n\n• Average rating (1-5 stars)\n• Number of rides completed\n\nThis helps you make informed decisions. You can also rate your ride partners after the trip.",
            category = FaqCategory.SAFETY
        ),
        FaqItem(
            question = "What if someone behaves inappropriately?",
            answer = "1. End the interaction immediately\n2. Report the user through the app\n3. If you feel unsafe during a ride, ask to be dropped at a safe public location\n4. Contact local authorities if necessary\n\nZero tolerance for harassment — violators are permanently banned.",
            category = FaqCategory.SAFETY
        ),

        // ─── PAYMENTS ───────────────────────────────────────────────────
        FaqItem(
            question = "How does payment work?",
            answer = "SAFAR does not handle payments. The app is for coordination only. Drivers and passengers arrange payment offline, typically:\n\n• Cash on the ride (most common)\n• Mobile wallet transfer (JazzCash, Easypaisa)\n• Bank transfer (for regular carpoolers)\n\nAgree on the method before the trip starts.",
            category = FaqCategory.PAYMENTS
        ),
        FaqItem(
            question = "Is there any in-app payment?",
            answer = "No. SAFAR is a community platform, not a commercial service. All payment happens directly between driver and passenger. This keeps the service free and flexible.",
            category = FaqCategory.PAYMENTS
        ),
        FaqItem(
            question = "What if the driver changes the price?",
            answer = "The price shown in the ride listing should be honored. If a driver tries to charge more, you can:\n\n• Politely decline and cancel\n• Report the ride for misleading pricing\n\nAlways confirm the price in chat before departing.",
            category = FaqCategory.PAYMENTS
        ),

        // ─── ACCOUNT & PROFILE ──────────────────────────────────────────
        FaqItem(
            question = "How do I update my profile?",
            answer = "1. Tap your profile icon in the bottom navigation\n2. Tap 'Edit Profile'\n3. Update your name, photo, or phone number\n4. Tap 'Save'\n\nA complete profile with a real photo builds trust in the community.",
            category = FaqCategory.ACCOUNT
        ),
        FaqItem(
            question = "Can I change my phone number?",
            answer = "Currently, phone numbers cannot be changed after signup as they are tied to account verification. If you need to change it, contact support through the 'Help & Support' section.",
            category = FaqCategory.ACCOUNT
        ),
        FaqItem(
            question = "How do I delete my account?",
            answer = "To permanently delete your account:\n\n1. Complete or cancel all active rides\n2. Go to Profile → Settings\n3. Tap 'Delete Account'\n4. Confirm deletion\n\nThis action is irreversible and deletes all your data.",
            category = FaqCategory.ACCOUNT
        ),
        FaqItem(
            question = "Why should I add a profile photo?",
            answer = "Profile photos help drivers and passengers recognize each other at pickup points. Accounts with real photos are more trusted and receive more requests.",
            category = FaqCategory.ACCOUNT
        ),

        // ─── TROUBLESHOOTING ────────────────────────────────────────────
        FaqItem(
            question = "I'm not receiving OTP codes",
            answer = "If OTP doesn't arrive:\n\n1. Check your signal strength\n2. Make sure you entered the correct number\n3. Wait 2-3 minutes (delays sometimes occur)\n4. Request a new OTP\n5. Check if your number is blocked from receiving SMS\n\nStill stuck? Contact your telecom provider.",
            category = FaqCategory.TROUBLESHOOTING
        ),
        FaqItem(
            question = "Rides aren't showing on the map",
            answer = "Common fixes:\n\n• Enable location permissions in phone settings\n• Make sure GPS is turned on\n• Check your internet connection\n• Zoom out — rides may be outside your current view\n• Refresh by pulling down on the list view",
            category = FaqCategory.TROUBLESHOOTING
        ),
        FaqItem(
            question = "Chat messages aren't sending",
            answer = "1. Check your internet connection\n2. Make sure you're still connected to the ride (driver hasn't canceled)\n3. Close and reopen the chat\n4. Restart the app\n\nIf the issue persists, the ride may have been deleted.",
            category = FaqCategory.TROUBLESHOOTING
        ),
        FaqItem(
            question = "App is crashing or freezing",
            answer = "Try these steps:\n\n1. Clear app cache (Settings → Apps → SAFAR → Clear Cache)\n2. Update to the latest version from Play Store\n3. Restart your device\n4. Uninstall and reinstall the app (your account data is safe)\n\nStill broken? Report the issue via 'Help & Support'.",
            category = FaqCategory.TROUBLESHOOTING
        ),
        FaqItem(
            question = "How do I contact support?",
            answer = "For issues not covered in this FAQ:\n\n• Tap 'Help & Support' in your profile\n• Use the 'Contact Support' button\n• Email: safar.support.app@gmail.com\n\nWe typically respond within 24-48 hours.",
            category = FaqCategory.TROUBLESHOOTING
        )
    )

    /**
     * Get all FAQs grouped by category.
     */
    fun getFaqsByCategory(): Map<FaqCategory, List<FaqItem>> {
        return ALL_FAQS.groupBy { it.category }
    }
}

/**
 * State holder for Help & FAQ screen.
 * Manages expanded FAQ items.
 */
class HelpStateHolder {
    
    /** Index of currently expanded FAQ item (-1 = none) */
    var expandedIndex by mutableIntStateOf(-1)
    
    /**
     * Toggles the expansion state of an FAQ item.
     */
    fun toggleFaq(index: Int) {
        expandedIndex = if (expandedIndex == index) -1 else index
    }
    
    /**
     * Collapses all FAQs.
     */
    fun collapseAll() {
        expandedIndex = -1
    }
}

/**
 * Sets up the Compose UI for HelpActivity.
 */
fun HelpActivity.setupComposeUI(stateHolder: HelpStateHolder) {
    setContent {
        SafarTheme(darkTheme = true) {
            HelpScreen(
                faqsByCategory = FaqDatabase.getFaqsByCategory(),
                expandedIndex = stateHolder.expandedIndex,
                onToggleFaq = { index -> stateHolder.toggleFaq(index) },
                onContactSupport = { openSupportEmail() },
                onBack = { onBackPressedDispatcher.onBackPressed() }
            )
        }
    }
}
