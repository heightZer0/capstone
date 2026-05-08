package com.example.capstone.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.capstone.data.local.entity.ErrorCodeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ErrorCodeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertErrorCode(errorCode: ErrorCodeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertErrorCodes(errorCodes: List<ErrorCodeEntity>)

    @Query("SELECT * FROM error_code ORDER BY error_code ASC")
    fun getAllErrorCodes(): Flow<List<ErrorCodeEntity>>

    @Query("SELECT * FROM error_code WHERE error_code = :errorCode")
    suspend fun getErrorCodeByCode(errorCode: String): ErrorCodeEntity?
}