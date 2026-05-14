package com.example.capstone.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.capstone.data.local.entity.ResultEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ResultDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResult(result: ResultEntity): Long

    @Query("SELECT * FROM result ORDER BY analyzed_time DESC")
    fun getAllResults(): Flow<List<ResultEntity>>

    @Query("SELECT * FROM result WHERE analysis_id = :analysisId")
    suspend fun getResultById(analysisId: Int): ResultEntity?

    @Query("SELECT * FROM result WHERE analyzed_time BETWEEN :start AND :end ORDER BY analyzed_time ASC")
    fun getResultsByDate(start: Long, end: Long): Flow<List<ResultEntity>>

    @Query("SELECT * FROM result ORDER BY analyzed_time DESC LIMIT :limit")
    fun getRecentResults(limit: Int): Flow<List<ResultEntity>>


    @Query("SELECT MAX(analysis_id) FROM result")
    suspend fun getLatestAnalysisId(): Int?
}