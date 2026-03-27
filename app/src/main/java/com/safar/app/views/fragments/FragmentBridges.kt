package com.safar.app.views.fragments

// ══════════════════════════════════════════════════════════════════════════════
// SAFAR - Fragment Bridges
// Navigation and communication bridges between fragments
// ══════════════════════════════════════════════════════════════════════════════

/**
 * This file provides bridge interfaces for fragment communication.
 * Individual fragment implementations are now in their own files:
 * 
 * - MapFragment.kt       : Premium map screen with Uber-style floating panels
 * - ListFragment.kt      : Rides listing with search and filtering
 * - InboxFragment.kt     : Messages and ride requests inbox
 * - AdBottomSheetFragment.kt : Ride detail bottom sheet overlay
 * 
 * All fragments use Jetpack Compose with Material 3 design system
 * and follow modern Android architecture patterns.
 */

/**
 * Interface for tab navigation callbacks
 */
interface TabNavigationListener {
    fun onTabSelected(tabIndex: Int)
}

/**
 * Interface for ride selection callbacks
 */
interface RideSelectionListener {
    fun onRideSelected(adId: String)
}

/**
 * Interface for chat navigation callbacks
 */
interface ChatNavigationListener {
    fun onChatOpen(chatRoomId: String, peerName: String)
}

/**
 * Interface for refresh callbacks
 */
interface RefreshListener {
    fun onRefreshRequested()
}
