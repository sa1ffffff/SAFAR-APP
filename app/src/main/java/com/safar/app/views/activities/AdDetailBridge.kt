package com.safar.app.views.activities

import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.safar.app.views.ui.screens.AdDetailScreen
import com.safar.app.views.ui.screens.AdDetailUiState
import com.safar.app.views.ui.screens.JoinRequestUiModel
import com.safar.app.views.ui.theme.SafarTheme

/**
 * State holder for ad detail screen.
 * Manages ride advertisement details and join requests.
 */
class AdDetailStateHolder {
    
    /** The current UI state */
    var state by mutableStateOf(AdDetailUiState())
        private set
    
    /** Whether action sheet is visible */
    var showActionSheet by mutableStateOf(false)
    
    /** Whether delete confirmation dialog is visible */
    var showDeleteDialog by mutableStateOf(false)
    
    /** Whether mark filled confirmation is visible */
    var showMarkFilledDialog by mutableStateOf(false)
    
    /** Currently processing request ID (for loading state) */
    var processingRequestId by mutableStateOf<String?>(null)
        private set
    
    /**
     * Sets the loading state.
     */
    fun setLoading(loading: Boolean) {
        state = state.copy(isLoading = loading)
    }
    
    /**
     * Sets the origin/pickup address.
     */
    fun setOrigin(origin: String) {
        state = state.copy(origin = origin)
    }
    
    /**
     * Sets the destination address.
     */
    fun setDestination(dest: String) {
        state = state.copy(destination = dest)
    }
    
    /**
     * Sets the departure time display string.
     */
    fun setDepartureTime(time: String) {
        state = state.copy(departureTime = time)
    }
    
    /**
     * Sets the available seats display string.
     */
    fun setSeats(seats: String) {
        state = state.copy(seats = seats)
    }
    
    /**
     * Sets the poster's name.
     */
    fun setPostedBy(name: String) {
        state = state.copy(postedBy = name)
    }
    
    /**
     * Sets the poster's phone number.
     */
    fun setPosterPhone(phone: String) {
        state = state.copy(posterPhone = phone)
    }
    
    /**
     * Sets the poster's rating.
     */
    fun setPosterRating(rating: Float) {
        state = state.copy(posterRating = rating)
    }
    
    /**
     * Sets the poster's avatar URL.
     */
    fun setPosterAvatar(avatarUrl: String?) {
        state = state.copy(posterAvatar = avatarUrl)
    }
    
    /**
     * Sets the vehicle type/name.
     */
    fun setVehicle(vehicle: String) {
        state = state.copy(vehicle = vehicle)
    }
    
    /**
     * Sets the ad status.
     */
    fun setStatus(status: String) {
        state = state.copy(status = status)
    }
    
    /**
     * Sets additional notes.
     */
    fun setNotes(notes: String) {
        state = state.copy(notes = notes)
    }
    
    /**
     * Sets the price per seat.
     */
    fun setPrice(price: String) {
        state = state.copy(price = price)
    }
    
    /**
     * Sets whether current user is the ad owner.
     */
    fun setIsOwner(isOwner: Boolean) {
        state = state.copy(isOwner = isOwner)
    }
    
    /**
     * Sets whether current user has already requested to join.
     */
    fun setHasRequested(hasRequested: Boolean) {
        state = state.copy(hasRequested = hasRequested)
    }
    
    /**
     * Sets the user's request status (if they've requested).
     */
    fun setUserRequestStatus(status: String) {
        state = state.copy(userRequestStatus = status)
    }
    
    /**
     * Sets the join requests list.
     */
    fun setRequests(list: List<JoinRequestUiModel>) {
        state = state.copy(joinRequests = list)
    }
    
    /**
     * Updates a single request's status.
     */
    fun updateRequestStatus(requestId: String, newStatus: String) {
        val updatedRequests = state.joinRequests.map { request ->
            if (request.id == requestId) request.copy(status = newStatus) else request
        }
        state = state.copy(joinRequests = updatedRequests)
        processingRequestId = null
    }
    
    /**
     * Removes a request from the list.
     */
    fun removeRequest(requestId: String) {
        val updatedRequests = state.joinRequests.filter { it.id != requestId }
        state = state.copy(joinRequests = updatedRequests)
        processingRequestId = null
    }
    
    /**
     * Sets the currently processing request.
     */
    fun setProcessingRequest(requestId: String?) {
        processingRequestId = requestId
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
    
    /**
     * Shows/hides the action sheet.
     */
    fun toggleActionSheet(show: Boolean) {
        showActionSheet = show
    }
    
    /**
     * Shows delete confirmation dialog.
     */
    fun showDeleteConfirmation() {
        showDeleteDialog = true
        showActionSheet = false
    }
    
    /**
     * Shows mark filled confirmation dialog.
     */
    fun showMarkFilledDialog() {
        showMarkFilledDialog = true
        showActionSheet = false
    }
    
    /**
     * Dismisses all dialogs.
     */
    fun dismissDialogs() {
        showDeleteDialog = false
        showMarkFilledDialog = false
        showActionSheet = false
    }
}

/**
 * Sets up the Compose UI for AdDetailActivity.
 * Uses light theme for detail screens following Uber's design pattern.
 *
 * @receiver The AdDetailActivity instance
 * @param stateHolder The state holder managing ad detail state
 */
fun AdDetailActivity.setupComposeUI(stateHolder: AdDetailStateHolder) {
    setContent {
        SafarTheme(darkTheme = false) {
            AdDetailScreen(
                uiState = stateHolder.state,
                showActionSheet = stateHolder.showActionSheet,
                showDeleteDialog = stateHolder.showDeleteDialog,
                showMarkFilledDialog = stateHolder.showMarkFilledDialog,
                processingRequestId = stateHolder.processingRequestId,
                onBack = { onBackPressedDispatcher.onBackPressed() },
                onShowActionSheet = { stateHolder.toggleActionSheet(true) },
                onDismissActionSheet = { stateHolder.toggleActionSheet(false) },
                onMarkFilled = {
                    stateHolder.showMarkFilledDialog = true
                },
                onConfirmMarkFilled = {
                    stateHolder.dismissDialogs()
                    stateHolder.setLoading(true)
                    handleMarkFilled()
                },
                onEditAd = {
                    stateHolder.toggleActionSheet(false)
                    handleEditAd()
                },
                onDeleteAd = {
                    stateHolder.showDeleteConfirmation()
                },
                onConfirmDelete = {
                    stateHolder.dismissDialogs()
                    stateHolder.setLoading(true)
                    handleDeleteAd()
                },
                onDismissDialogs = { stateHolder.dismissDialogs() },
                onShareAd = {
                    stateHolder.toggleActionSheet(false)
                    handleShareAd()
                },
                onSendRequest = {
                    stateHolder.setLoading(true)
                    handleSendRequest()
                },
                onCancelRequest = {
                    stateHolder.setLoading(true)
                    handleCancelRequest()
                },
                onAcceptRequest = { requestId ->
                    stateHolder.setProcessingRequest(requestId)
                    handleAccept(requestId)
                },
                onRejectRequest = { requestId ->
                    stateHolder.setProcessingRequest(requestId)
                    handleReject(requestId)
                },
                onContactPoster = { handleContactPoster() },
                onCallPoster = { handleCallPoster() },
                onChatWithPoster = { handleChatWithPoster() },
                onReportAd = { handleReportAd() }
            )
        }
    }
}
