package com.example.capstone.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.capstone.data.local.dao.DispenseLogDao
import com.example.capstone.data.local.dao.ErrorCodeDao
import com.example.capstone.data.local.dao.ErrorObjectDao
import com.example.capstone.data.local.dao.ResultDao
import com.example.capstone.data.local.entity.DispenseLogEntity
import com.example.capstone.data.local.entity.ErrorCodeEntity
import com.example.capstone.data.local.entity.ErrorObjectEntity
import com.example.capstone.data.local.entity.ResultEntity

@Database(
    entities = [
        DispenseLogEntity::class,
        ResultEntity::class,
        ErrorCodeEntity::class,
        ErrorObjectEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dispenseLogDao(): DispenseLogDao
    abstract fun resultDao(): ResultDao
    abstract fun errorCodeDao(): ErrorCodeDao
    abstract fun errorObjectDao(): ErrorObjectDao
}