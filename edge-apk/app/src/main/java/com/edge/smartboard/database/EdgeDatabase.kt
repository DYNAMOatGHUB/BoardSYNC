package com.edge.smartboard.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.edge.smartboard.models.Session

@Database(
    entities = [Session::class],
    version = 1,
    exportSchema = false
)
abstract class EdgeDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
}
