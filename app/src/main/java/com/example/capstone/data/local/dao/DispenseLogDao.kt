package com.example.capstone.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.capstone.data.local.entity.DispenseLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DispenseLogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: DispenseLogEntity): Long

    @Update
    suspend fun updateLog(log: DispenseLogEntity)

    @Query("SELECT * FROM dispense_log ORDER BY start_time DESC")
    fun getAllLogs(): Flow<List<DispenseLogEntity>>

    @Query("SELECT * FROM dispense_log WHERE log_id = :logId")
    suspend fun getLogById(logId: Int): DispenseLogEntity?
}