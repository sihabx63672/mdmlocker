package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "locker_settings")
data class LockerEntity(
    @PrimaryKey val id: Int = 1,
    val isLocked: Boolean = false,
    val pinCode: String = "1234",
    val wallpaperTheme: String = "COSMIC_MIDNIGHT",
    val lockMessage: String = "REMOTE LOCK ACTIVATED BY ADMINISTRATOR",
    val adminPassword: String = "admin123",
    val lastLockTimestamp: Long = System.currentTimeMillis(),
    val showPowerMenuBlocker: Boolean = true
)
