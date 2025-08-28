package com.mehfooz.accounts.app.data

import androidx.room.*
import com.mehfooz.accounts.app.domain.ActivationRecord

/* ---------------------------
   Minimal Users table entity
   --------------------------- */
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val uid: String,
    val email: String? = null,
    /** 1 = enabled, 0 = not enabled */
    val enabled: Int = 0
)

/* ---------------------------
   DAO for activation reads/writes
   --------------------------- */
@Dao
interface ActivationDao {

    /* Read activation state + (optional) subscription snapshot
       NOTE: Adjust table/column names if yours differ. */
    @Query(
        """
        SELECT 
            u.uid                                       AS uid,
            CASE WHEN IFNULL(u.enabled, 0) = 1 THEN 1 ELSE 0 END 
                                                        AS enabled,
            s.status                                    AS subscriptionStatus,
            s.expires_at                                AS subscriptionExpiresAt
        FROM users u
        LEFT JOIN subscriptions s ON s.uid = u.uid
        WHERE u.uid = :uid
        LIMIT 1
        """
    )
    suspend fun getActivation(uid: String): ActivationRecord?

    /* Insert a stub user row if it doesn't exist (enabled = 0) */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(user: UserEntity): Long

    /* Update email (keeps enabled as-is) */
    @Query("UPDATE users SET email = COALESCE(:email, email) WHERE uid = :uid")
    suspend fun updateEmailIfPresent(uid: String, email: String?)

    /* (Optional) toggle enabled flag if you need it for admin tools later */
    @Query("UPDATE users SET enabled = :enabled WHERE uid = :uid")
    suspend fun setEnabled(uid: String, enabled: Int)
}