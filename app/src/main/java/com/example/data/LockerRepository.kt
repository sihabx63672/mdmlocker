package com.example.data

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LockerRepository(context: Context) {
    private val lockerDao = LockerDatabase.getDatabase(context).lockerDao()

    val settingsFlow: Flow<LockerEntity> = lockerDao.getSettingsFlow().map { entity ->
        entity ?: LockerEntity()
    }

    suspend fun getSettings(): LockerEntity {
        return lockerDao.getSettings() ?: LockerEntity().also { defaultEntity ->
            lockerDao.saveSettings(defaultEntity)
        }
    }

    suspend fun saveSettings(entity: LockerEntity) {
        lockerDao.saveSettings(entity)
    }

    suspend fun updateLockState(isLocked: Boolean) {
        val current = getSettings()
        saveSettings(current.copy(isLocked = isLocked, lastLockTimestamp = System.currentTimeMillis()))
    }

    suspend fun updatePinCode(newPin: String) {
        val current = getSettings()
        saveSettings(current.copy(pinCode = newPin))
    }

    suspend fun updateWallpaperTheme(theme: String) {
        val current = getSettings()
        saveSettings(current.copy(wallpaperTheme = theme))
    }

    suspend fun updateLockMessage(message: String) {
        val current = getSettings()
        saveSettings(current.copy(lockMessage = message))
    }

    suspend fun updateAdminPassword(password: String) {
        val current = getSettings()
        saveSettings(current.copy(adminPassword = password))
    }
}
