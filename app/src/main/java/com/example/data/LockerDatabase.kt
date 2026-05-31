package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [LockerEntity::class], version = 1, exportSchema = false)
abstract class LockerDatabase : RoomDatabase() {
    abstract fun lockerDao(): LockerDao

    companion object {
        @Volatile
        private var INSTANCE: LockerDatabase? = null

        fun getDatabase(context: Context): LockerDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LockerDatabase::class.java,
                    "locker_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
