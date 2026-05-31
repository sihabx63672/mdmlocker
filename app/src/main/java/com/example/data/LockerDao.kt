package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface LockerDao {
    @Query("SELECT * FROM locker_settings WHERE id = 1 LIMIT 1")
    fun getSettingsFlow(): Flow<LockerEntity?>

    @Query("SELECT * FROM locker_settings WHERE id = 1 LIMIT 1")
    suspend fun getSettings(): LockerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSettings(settings: LockerEntity)
}
