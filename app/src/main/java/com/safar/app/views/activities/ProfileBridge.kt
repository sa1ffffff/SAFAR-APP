package com.safar.app.views.activities

import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.safar.app.views.ui.screens.ProfileScreen
import com.safar.app.views.ui.screens.ProfileUiState
import com.safar.app.views.ui.theme.SafarTheme

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// PROFILE BRIDGE
// Connects Java ProfileActivity to Jetpack Compose ProfileScreen
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
//
// INTEGRATION GUIDE:
// ─────────────────────────────────────────────────────────────────────────────
// In ProfileActivity.java:
//
//   1. Create state holder:
//      private ProfileStateHolder stateHolder = new ProfileStateHolder();
//
//   2. In onCreate():
//      REMOVE:  setContentView(R.layout.activity_profile)
//      ADD:     ProfileBridgeKt.setupComposeUI(this, stateHolder)
//
//   3. Update profile data from API:
//      stateHolder.updateName("John Doe");
//      stateHolder.updatePhone("+92 300 1234567");
//      stateHolder.updateRating(4.8f, 156);
//      stateHolder.updateTotalRides(45);
//
//   4. Handle loading state:
//      stateHolder.updateLoading(true);
//      // ...fetch data...
//      stateHolder.updateLoading(false);
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Profile menu item types for navigation.
 */
enum class ProfileMenuItem {
    MY_RIDES,
    PAYMENT_METHODS,
    SAVED_PLACES,
    SAFETY,
    HELP,
    SETTINGS,
    ABOUT,
    LOGOUT
}

/**
 * User verification status.
 */
enum class VerificationStatus {
    UNVERIFIED,
    PENDING,
    VERIFIED
}

/**
 * Extended profile data model.
 */
data class UserProfile(
    val id: String = "",
    val name: String = "",
    val phone: String = "",
    val email: String? = null,
    val bio: String = "",
    val avatarUrl: String? = null,
    val rating: Float = 0f,
    val ratingCount: Int = 0,
    val totalRides: Int = 0,
    val memberSince: String = "",
    val verificationStatus: VerificationStatus = VerificationStatus.UNVERIFIED,
    val isDriver: Boolean = false
)

/**
 * State holder for profile screen.
 * Manages user profile data and UI state.
 *
 * Java Access Pattern:
 * ```java
 * ProfileStateHolder stateHolder = new ProfileStateHolder();
 * 
 * // Reading state
 * ProfileUiState state = stateHolder.getState();
 * String name = state.getName();
 * 
 * // Updating data
 * stateHolder.updateName("John Doe");
 * stateHolder.updatePhone("+92 300 1234567");
 * stateHolder.updateRating(4.8f, 156);
 * stateHolder.updateTotalRides(45);
 * stateHolder.updateLoading(false);
 * 
 * // Full profile update
 * stateHolder.updateProfile(userProfile);
 * ```
 */
class ProfileStateHolder {
    
    /** The current profile UI state */
    var state by mutableStateOf(ProfileUiState())
        private set
    
    /** Whether the logout dialog is showing */
    var showLogoutDialog by mutableStateOf(false)
    
    /** Whether edit dialog is showing */
    var showEditDialog by mutableStateOf(false)
    
    /** Temporary edit values */
    var editName by mutableStateOf("")
        internal set
    var editBio by mutableStateOf("")
        internal set
    
    /**
     * Updates the loading state.
     */
    fun updateLoading(loading: Boolean) {
        state = state.copy(isLoading = loading)
    }
    
    /**
     * Updates the photo uploading state.
     */
    fun updateUploading(uploading: Boolean) {
        state = state.copy(isUploadingPhoto = uploading)
    }
    
    /**
     * Updates the user's display name.
     */
    fun updateName(name: String) {
        state = state.copy(name = name)
    }
    
    /**
     * Updates the user's bio.
     */
    fun updateBio(bio: String) {
        state = state.copy(bio = bio)
    }
    
    /**
     * Updates the user's phone number.
     */
    fun updatePhone(phone: String) {
        state = state.copy(phone = phone)
    }
    
    /**
     * Updates the user's email address.
     */
    fun updateEmail(email: String) {
        state = state.copy(email = email)
    }
    
    /**
     * Updates the user's avatar URL.
     */
    fun updateAvatar(avatarUrl: String?) {
        state = state.copy(avatarUrl = avatarUrl)
    }
    
    /**
     * Updates the total rides count.
     */
    fun updateTotalRides(count: Int) {
        state = state.copy(totalRides = count)
    }
    
    /**
     * Updates the user's rating.
     */
    fun updateRating(rating: Float, count: Int) {
        state = state.copy(rating = rating, ratingCount = count)
    }
    
    /**
     * Updates member since date.
     */
    fun updateMemberSince(date: String) {
        state = state.copy(memberSince = date)
    }
    
    /**
     * Updates the full profile from a UserProfile object.
     */
    fun updateProfile(profile: UserProfile) {
        state = state.copy(
            name = profile.name,
            phone = profile.phone,
            email = profile.email,
            bio = profile.bio,
            avatarUrl = profile.avatarUrl,
            rating = profile.rating,
            ratingCount = profile.ratingCount,
            totalRides = profile.totalRides,
            memberSince = profile.memberSince,
            isLoading = false
        )
    }
    
    /**
     * Shows the edit profile dialog.
     */
    fun showEditProfileDialog() {
        editName = state.name
        editBio = state.bio
        showEditDialog = true
    }
    
    /**
     * Dismisses the edit dialog.
     */
    fun dismissEditDialog() {
        showEditDialog = false
        editName = ""
        editBio = ""
    }
    
    /**
     * Shows the logout confirmation dialog.
     */
    fun showLogoutConfirmation() {
        showLogoutDialog = true
    }
    
    /**
     * Dismisses the logout dialog.
     */
    fun dismissLogoutDialog() {
        showLogoutDialog = false
    }
    
    /**
     * Sets an error message.
     */
    fun setError(message: String) {
        state = state.copy(errorMessage = message, isLoading = false)
    }
    
    /**
     * Clears the error message.
     */
    fun clearError() {
        state = state.copy(errorMessage = null)
    }
}

/**
 * Sets up the Compose UI for ProfileActivity.
 * Uses light theme for profile screen following Uber's design pattern.
 *
 * @receiver The ProfileActivity instance
 * @param stateHolder The state holder managing profile state
 */
fun ProfileActivity.setupComposeUI(stateHolder: ProfileStateHolder) {
    setContent {
        SafarTheme(darkTheme = false) {
            ProfileScreen(
                state = stateHolder.state,
                showLogoutDialog = stateHolder.showLogoutDialog,
                showEditDialog = stateHolder.showEditDialog,
                editName = stateHolder.editName,
                editBio = stateHolder.editBio,
                onEditNameChange = { stateHolder.editName = it },
                onEditBioChange = { stateHolder.editBio = it },
                onShowEditDialog = { stateHolder.showEditProfileDialog() },
                onDismissEditDialog = { stateHolder.dismissEditDialog() },
                onSaveEdit = {
                    handleSaveProfile(stateHolder.editName, stateHolder.editBio)
                    stateHolder.dismissEditDialog()
                },
                onChangePhoto = { handleChangePhoto() },
                onAvatarClick = { handleAvatarClick() },
                onMyRides = { navigateToMyRides() },
                onPaymentMethods = { navigateToPaymentMethods() },
                onSavedPlaces = { navigateToSavedPlaces() },
                onSafety = { navigateToSafety() },
                onHelp = { navigateToHelp() },
                onSettings = { navigateToSettings() },
                onAbout = { navigateToAbout() },
                onLogout = { stateHolder.showLogoutConfirmation() },
                onConfirmLogout = {
                    stateHolder.dismissLogoutDialog()
                    handleLogout()
                },
                onDismissLogout = { stateHolder.dismissLogoutDialog() },
                onBack = { onBackPressedDispatcher.onBackPressed() }
            )
        }
    }
}
