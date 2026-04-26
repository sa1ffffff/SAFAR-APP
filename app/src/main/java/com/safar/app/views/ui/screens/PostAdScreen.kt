package com.safar.app.views.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.*
import com.safar.app.utils.PhoneFormatUtils
import com.safar.app.views.ui.theme.*

data class PostAdUiState(
    val originAddress: String = "",
    val destAddress: String = "",
    val dateTimeLabel: String = "",
    val phone: String = "",
    val seats: String = "",
    val vehicle: String = "",
    val notes: String = "",
    val price: String = "",
    val isLoading: Boolean = false,
    val originPicked: Boolean = false,
    val destPicked: Boolean = false,
    val dateTimePicked: Boolean = false,
    val originLat: Double = 0.0,
    val originLng: Double = 0.0,
    val destLat: Double = 0.0,
    val destLng: Double = 0.0,
    val departureTimestamp: Long = 0L,
    val errorMessage: String? = null
)

/**
 * SAFAR POST AD SCREEN
 * Enterprise-level ride posting interface with Uber-inspired design
 * 
 * Features:
 * - Clean sectioned layout
 * - Location picker cards
 * - Schedule selection
 * - Form validation
 * - Premium submit button
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostAdScreen(
    state: PostAdUiState = PostAdUiState(),
    fieldErrors: Map<String, String> = emptyMap(),
    isSubmitting: Boolean = false,
    vehicleTypes: List<String> = emptyList(),
    onPhoneChange: (String) -> Unit = {},
    onSeatsChange: (String) -> Unit = {},
    onVehicleChange: (String) -> Unit = {},
    onNotesChange: (String) -> Unit = {},
    onPriceChange: (String) -> Unit = {},
    onPickOrigin: () -> Unit = {},
    onPickDest: () -> Unit = {},
    onPickDateTime: () -> Unit = {},
    onPost: () -> Unit = {},
    onBack: () -> Unit = {}
) {
    val effectiveLoading = state.isLoading || isSubmitting
    val hasLivePhoneFormatError =
        state.phone.isNotBlank() && !PhoneFormatUtils.isValidWhatsappLocalNumber(state.phone)
    val phoneErrorMessage = when {
        hasLivePhoneFormatError -> "Format must be 03xx-xxxxxxx"
        else -> fieldErrors["phone"]
    }
    val canAttemptPost = state.originPicked &&
            state.destPicked &&
            state.dateTimePicked &&
            state.seats.isNotBlank() &&
            state.vehicle.isNotBlank() &&
            state.phone.isNotBlank()
    val bannerErrorMessage = state.errorMessage
        ?: fieldErrors.entries.firstOrNull { it.key != "phone" }?.value

    SafarTheme(darkTheme = true) {
        Scaffold(
            containerColor = SafarBlack,
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                "Post a Ride",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = SafarTextPrimary
                            )
                            Text(
                                "Share your trip with others",
                                fontSize = 12.sp,
                                color = SafarTextSecondary
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.Rounded.Close,
                                "Close",
                                tint = SafarTextPrimary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = SafarDarkSurface
                    )
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
            ) {
                // Error Banner
                AnimatedVisibility(
                    visible = bannerErrorMessage != null,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    bannerErrorMessage?.let { error ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = SafarError.copy(alpha = 0.12f)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Rounded.ErrorOutline,
                                    null,
                                    tint = SafarError,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(error, color = SafarError, fontSize = 13.sp)
                            }
                        }
                    }
                }

                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Spacer(modifier = Modifier.height(8.dp))

                    // ── Route Section ─────────────────────────────────────────
                    SectionHeader(
                        icon = Icons.Rounded.Route,
                        title = "ROUTE"
                    )

                    LocationPickerCard(
                        label = "From",
                        placeholder = "Tap to set pickup location",
                        value = state.originAddress,
                        isPicked = state.originPicked,
                        dotColor = SafarSuccess,
                        onClick = onPickOrigin
                    )

                    LocationPickerCard(
                        label = "To",
                        placeholder = "Tap to set destination",
                        value = state.destAddress,
                        isPicked = state.destPicked,
                        dotColor = SafarGreen,
                        onClick = onPickDest
                    )

                    // ── Schedule Section ──────────────────────────────────────
                    SectionHeader(
                        icon = Icons.Rounded.Schedule,
                        title = "SCHEDULE"
                    )

                    SchedulePickerCard(
                        value = state.dateTimeLabel,
                        isPicked = state.dateTimePicked,
                        onClick = onPickDateTime
                    )

                    // ── Details Section ───────────────────────────────────────
                    SectionHeader(
                        icon = Icons.Rounded.Info,
                        title = "DETAILS"
                    )

                    // Seats & Price Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        FormInputField(
                            value = state.seats,
                            onValueChange = onSeatsChange,
                            label = "Seats",
                            placeholder = "e.g. 3",
                            icon = Icons.Rounded.EventSeat,
                            keyboardType = KeyboardType.Number,
                            modifier = Modifier.weight(1f)
                        )

                        PriceInputField(
                            value = state.price,
                            onValueChange = onPriceChange,
                            label = "Price/Seat",
                            placeholder = "Optional",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    FormInputField(
                        value = state.phone,
                        onValueChange = onPhoneChange,
                        label = "Contact Phone",
                        placeholder = "e.g. 0300-1234567",
                        icon = Icons.Rounded.Phone,
                        keyboardType = KeyboardType.Phone,
                        isError = phoneErrorMessage != null,
                        supportingError = phoneErrorMessage
                    )

                    FormInputField(
                        value = state.vehicle,
                        onValueChange = onVehicleChange,
                        label = "Vehicle",
                        placeholder = "e.g. Honda Civic, Silver",
                        icon = Icons.Rounded.DirectionsCar
                    )

                    // Notes
                    OutlinedTextField(
                        value = state.notes,
                        onValueChange = onNotesChange,
                        label = { Text("Notes (Optional)", color = SafarTextSecondary) },
                        placeholder = {
                            Text(
                                "Any additional details for riders...",
                                color = SafarTextTertiary
                            )
                        },
                        minLines = 3,
                        maxLines = 5,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SafarGreen,
                            unfocusedBorderColor = SafarDivider,
                            focusedContainerColor = SafarDarkCard,
                            unfocusedContainerColor = SafarDarkCard,
                            focusedTextColor = SafarTextPrimary,
                            unfocusedTextColor = SafarTextPrimary,
                            focusedLabelColor = SafarGreen,
                            cursorColor = SafarGreen
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // ── Submit Button ─────────────────────────────────────────
                    Button(
                        onClick = onPost,
                        enabled = canAttemptPost && !effectiveLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SafarGreen,
                            contentColor = SafarBlack,
                            disabledContainerColor = SafarGreen.copy(alpha = 0.25f),
                            disabledContentColor = SafarBlack.copy(alpha = 0.25f)
                        )
                    ) {
                        if (effectiveLoading) {
                            CircularProgressIndicator(
                                color = SafarBlack,
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.5.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "Posting...",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        } else {
                            Icon(
                                Icons.Rounded.Add,
                                null,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Post Ride",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    icon: ImageVector,
    title: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Icon(
            icon,
            null,
            tint = SafarGreen,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            title,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            color = SafarTextTertiary,
            letterSpacing = 1.5.sp
        )
    }
}

@Composable
private fun LocationPickerCard(
    label: String,
    placeholder: String,
    value: String,
    isPicked: Boolean,
    dotColor: Color,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SafarDarkCard),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Dot indicator
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(
                        if (isPicked) dotColor else SafarTextTertiary.copy(alpha = 0.3f)
                    )
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    label.uppercase(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = SafarTextTertiary,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = value.ifEmpty { placeholder },
                    color = if (isPicked) SafarTextPrimary else SafarTextTertiary,
                    fontWeight = if (isPicked) FontWeight.Medium else FontWeight.Normal,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }

            Icon(
                imageVector = if (isPicked) Icons.Rounded.CheckCircle else Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = if (isPicked) dotColor else SafarTextTertiary,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun SchedulePickerCard(
    value: String,
    isPicked: Boolean,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SafarDarkCard),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isPicked) SafarGreen.copy(alpha = 0.12f)
                        else SafarDarkSurface
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.CalendarMonth,
                    null,
                    tint = if (isPicked) SafarGreen else SafarTextTertiary,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "DEPARTURE TIME",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = SafarTextTertiary,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = value.ifEmpty { "Select date and time" },
                    color = if (isPicked) SafarTextPrimary else SafarTextTertiary,
                    fontWeight = if (isPicked) FontWeight.Medium else FontWeight.Normal,
                    fontSize = 14.sp
                )
            }

            Icon(
                imageVector = if (isPicked) Icons.Rounded.CheckCircle else Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = if (isPicked) SafarGreen else SafarTextTertiary,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun FormInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    icon: ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text,
    isError: Boolean = false,
    supportingError: String? = null,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = SafarTextSecondary, fontSize = 12.sp) },
        placeholder = { Text(placeholder, color = SafarTextTertiary.copy(alpha = 0.6f)) },
        leadingIcon = {
            Icon(
                icon,
                null,
                tint = SafarTextTertiary,
                modifier = Modifier.size(20.dp)
            )
        },
        singleLine = true,
        isError = isError,
        shape = RoundedCornerShape(14.dp),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        supportingText = if (supportingError != null) {
            {
                Text(
                    text = supportingError,
                    color = SafarError,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        } else null,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = SafarGreen,
            unfocusedBorderColor = SafarDivider,
            focusedContainerColor = SafarDarkCard,
            unfocusedContainerColor = SafarDarkCard,
            focusedTextColor = SafarTextPrimary,
            unfocusedTextColor = SafarTextPrimary,
            focusedLabelColor = SafarGreen,
            cursorColor = SafarGreen
        ),
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
private fun PriceInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = SafarTextSecondary, fontSize = 12.sp) },
        placeholder = { Text(placeholder, color = SafarTextTertiary.copy(alpha = 0.6f)) },
        leadingIcon = {
            Text(
                "Rs",
                color = SafarTextTertiary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 4.dp)
            )
        },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = SafarGreen,
            unfocusedBorderColor = SafarDivider,
            focusedContainerColor = SafarDarkCard,
            unfocusedContainerColor = SafarDarkCard,
            focusedTextColor = SafarTextPrimary,
            unfocusedTextColor = SafarTextPrimary,
            focusedLabelColor = SafarGreen,
            cursorColor = SafarGreen
        ),
        modifier = modifier.fillMaxWidth()
    )
}
