package com.mehfooz.accounts.app.domain

/**
 * Minimal DB row shape we need from Room for activation.
 * (Step 2 will provide the actual @Query to return this.)
 */
data class ActivationRecord(
    val uid: String,          // Firebase UID (preferred key)
    val enabled: Boolean,     // 1 = active, 0 = not active
    // Optional subscription fields (keep for future logic; can be null safely)
    val subscriptionStatus: String? = null,   // e.g., "active", "expired"
    val subscriptionExpiresAt: String? = null // ISO date/time if you have it
)

/**
 * High-level result your app cares about after checking the DB.
 * ViewModels / Screens will branch on this.
 */
sealed class ActivationResult {
    data class Enabled(val uid: String) : ActivationResult()
    object NotEnabled : ActivationResult()
    /** Use when the user row isn't in DB yet (first login). */
    object NotFound : ActivationResult()
    data class Error(val message: String) : ActivationResult()
}

/**
 * Contract for the activation workflow. Step 3/4 will implement these.
 */
interface ActivationRepository {
    /**
     * Ensure there is at least a stub row for this UID (enabled=false initially).
     * Safe to call multiple times; should be idempotent.
     */
    suspend fun ensureUserRow(uid: String, email: String?): Result<Unit>

    /**
     * Read activation state from DB (users [+ subscription] tables).
     */
    suspend fun checkActivation(uid: String): ActivationResult
}