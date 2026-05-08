package com.example.capstone.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.capstone.data.local.entity.ErrorObjectEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ErrorObjectDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertErrorObject(errorObject: ErrorObjectEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertErrorObjects(errorObjects: List<ErrorObjectEntity>)

    @Query("SELECT * FROM error_object WHERE analysis_id = :analysisId ORDER BY pouch_no ASC")
    fun getErrorsByAnalysisId(analysisId: Int): Flow<List<ErrorObjectEntity>>

    @Query("SELECT COUNT(*) FROM error_object WHERE analysis_id = :analysisId")
    suspend fun getErrorCountByAnalysisId(analysisId: Int): Int
}