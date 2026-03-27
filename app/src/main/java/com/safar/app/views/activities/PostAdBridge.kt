package com.safar.app.views.activities

import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.safar.app.views.ui.screens.PostAdScreen
import com.safar.app.views.ui.screens.PostAdUiState
import com.safar.app.views.ui.theme.SafarTheme

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// POST AD BRIDGE
// Connects Java PostAdActivity to Jetpack Compose PostAdScreen
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
//
// INTEGRATION GUIDE:
// ─────────────────────────────────────────────────────────────────────────────
// In PostAdActivity.java:
//
//   1. Create state holder:
//      private PostAdStateHolder stateHolder = new PostAdStateHolder();
//
//   2. In onCreate():
//      REMOVE:  setContentView(R.layout.activity_post_ad)
//      ADD:     PostAdBridgeKt.setupComposeUI(this, stateHolder)
//
//   3. Update fields after location picker results:
//      stateHolder.updateOrigin("Johar Town, Lahore");
//      stateHolder.updateDestination("DHA Phase 5, Lahore");
//
//   4. Update date/time after picker:
//      stateHolder.updateDateTimeLabel("Today, 3:00 PM");
//
//   5. Access all form data for submission:
//      PostAdUiState state = stateHolder.getState();
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Vehicle type options for ride posting.
 */
enum class VehicleType(val displayName: String, val icon: String) {
    CAR("Car", "directions_car"),
    BIKE("Bike", "two_wheeler"),
    SUV("SUV", "airport_shuttle"),
    VAN("Van", "airport_shuttle")
}

/**
 * Form validation result.
 */
data class FormValidation(
    val isValid: Boolean,
    val errors: Map<String, String> = emptyMap()
) {
    companion object {
        val VALID = FormValidation(true)
        
        fun invalid(vararg errors: Pair<String, String>) = FormValidation(
            isValid = false,
            errors = errors.toMap()
        )
    }
}

/**
 * State holder for post ad screen.
 * Manages form state for creating ride advertisements.
 *
 * Java Access Pattern:
 * ```java
 * PostAdStateHolder stateHolder = new PostAdStateHolder();
 * 
 * // Reading state
 * PostAdUiState state = stateHolder.getState();
 * String origin = state.getOriginAddress();
 * 
 * // Updating fields
 * stateHolder.updateOrigin("Johar Town, Lahore");
 * stateHolder.updateDestination("DHA Phase 5");
 * stateHolder.updateDateTimeLabel("Today, 3:00 PM");
 * stateHolder.updateSeats("2");
 * stateHolder.updateVehicle("Car");
 * stateHolder.updatePhone("+923001234567");
 * stateHolder.updateNotes("AC available");
 * 
 * // Validation
 * FormValidation validation = stateHolder.validate();
 * if (validation.isValid()) { ... }
 * ```
 */
class PostAdStateHolder {
    
    /** The current form state */
    var state by mutableStateOf(PostAdUiState())
        private set
    
    /** Individual field errors */
    var fieldErrors by mutableStateOf<Map<String, String>>(emptyMap())
        private set
    
    /** Whether form is being submitted */
    var isSubmitting by mutableStateOf(false)
    
    /**
     * Updates the loading state.
     */
    fun updateLoading(loading: Boolean) {
        state = state.copy(isLoading = loading)
    }
    
    /**
     * Updates the origin/pickup address.
     */
    fun updateOrigin(address: String, lat: Double = 0.0, lng: Double = 0.0) {
        state = state.copy(
            originAddress = address,
            originPicked = true,
            originLat = lat,
            originLng = lng
        )
        clearFieldError("origin")
    }
    
    /**
     * Updates the destination address.
     */
    fun updateDestination(address: String, lat: Double = 0.0, lng: Double = 0.0) {
        state = state.copy(
            destAddress = address,
            destPicked = true,
            destLat = lat,
            destLng = lng
        )
        clearFieldError("destination")
    }
    
    /**
     * Updates the departure date/time label.
     */
    fun updateDateTimeLabel(label: String, timestamp: Long = 0L) {
        state = state.copy(
            dateTimeLabel = label,
            dateTimePicked = true,
            departureTimestamp = timestamp
        )
        clearFieldError("dateTime")
    }
    
    /**
     * Updates the phone number.
     */
    fun updatePhone(phone: String) {
        state = state.copy(phone = phone)
        clearFieldError("phone")
    }
    
    /**
     * Updates the available seats.
     */
    fun updateSeats(seats: String) {
        state = state.copy(seats = seats)
        clearFieldError("seats")
    }
    
    /**
     * Updates the vehicle type.
     */
    fun updateVehicle(vehicle: String) {
        state = state.copy(vehicle = vehicle)
        clearFieldError("vehicle")
    }
    
    /**
     * Updates additional notes.
     */
    fun updateNotes(notes: String) {
        state = state.copy(notes = notes)
    }
    
    /**
     * Updates the price per seat (if applicable).
     */
    fun updatePrice(price: String) {
        state = state.copy(price = price)
    }
    
    /**
     * Validates the form and returns validation result.
     */
    fun validate(): FormValidation {
        val errors = mutableMapOf<String, String>()
        
        if (!state.originPicked || state.originAddress.isBlank()) {
            errors["origin"] = "Please select pickup location"
        }
        
        if (!state.destPicked || state.destAddress.isBlank()) {
            errors["destination"] = "Please select destination"
        }
        
        if (!state.dateTimePicked || state.dateTimeLabel.isBlank()) {
            errors["dateTime"] = "Please select departure time"
        }
        
        if (state.phone.isBlank() || state.phone.length < 10) {
            errors["phone"] = "Please enter a valid phone number"
        }
        
        val seatsNum = state.seats.toIntOrNull()
        if (seatsNum == null || seatsNum < 1 || seatsNum > 8) {
            errors["seats"] = "Please enter valid seats (1-8)"
        }
        
        if (state.vehicle.isBlank()) {
            errors["vehicle"] = "Please select vehicle type"
        }
        
        fieldErrors = errors
        return FormValidation(
            isValid = errors.isEmpty(),
            errors = errors
        )
    }
    
    /**
     * Clears a specific field error.
     */
    private fun clearFieldError(field: String) {
        if (fieldErrors.containsKey(field)) {
            fieldErrors = fieldErrors - field
        }
    }
    
    /**
     * Clears all field errors.
     */
    fun clearAllErrors() {
        fieldErrors = emptyMap()
    }
    
    /**
     * Resets the form to initial state.
     */
    fun reset() {
        state = PostAdUiState()
        fieldErrors = emptyMap()
        isSubmitting = false
    }
    
    /**
     * Sets a submission error.
     */
    fun setSubmissionError(message: String) {
        state = state.copy(errorMessage = message)
        isSubmitting = false
    }
    
    /**
     * Clears submission error.
     */
    fun clearSubmissionError() {
        state = state.copy(errorMessage = null)
    }
}

/**
 * Sets up the Compose UI for PostAdActivity.
 *
 * @receiver The PostAdActivity instance
 * @param stateHolder The state holder managing form state
 */
fun PostAdActivity.setupComposeUI(stateHolder: PostAdStateHolder) {
    setContent {
        SafarTheme(darkTheme = true) {
            PostAdScreen(
                state = stateHolder.state,
                fieldErrors = stateHolder.fieldErrors,
                isSubmitting = stateHolder.isSubmitting,
                vehicleTypes = VehicleType.entries.map { it.displayName },
                onPhoneChange = { stateHolder.updatePhone(it) },
                onSeatsChange = { stateHolder.updateSeats(it) },
                onVehicleChange = { stateHolder.updateVehicle(it) },
                onNotesChange = { stateHolder.updateNotes(it) },
                onPriceChange = { stateHolder.updatePrice(it) },
                onPickOrigin = { launchOriginPicker() },
                onPickDest = { launchDestPicker() },
                onPickDateTime = { showDatePicker() },
                onPost = {
                    val validation = stateHolder.validate()
                    if (validation.isValid) {
                        stateHolder.isSubmitting = true
                        validateAndPost()
                    }
                },
                onBack = { onBackPressedDispatcher.onBackPressed() }
            )
        }
    }
}
