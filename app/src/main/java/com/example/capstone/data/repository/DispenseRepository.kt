package com.example.capstone.data.repository

import com.example.capstone.data.local.dao.DispenseLogDao
import com.example.capstone.data.local.dao.ErrorCodeDao
import com.example.capstone.data.local.dao.ErrorObjectDao
import com.example.capstone.data.local.dao.ResultDao
import com.example.capstone.data.local.entity.DispenseLogEntity
import com.example.capstone.data.local.entity.ErrorObjectEntity
import com.example.capstone.data.local.entity.ResultEntity
import kotlinx.coroutines.flow.Flow

class DispenseRepository(
    private val dispenseLogDao: DispenseLogDao,
    private val resultDao: ResultDao,
    private val errorCodeDao: ErrorCodeDao,
    private val errorObjectDao: ErrorObjectDao
) {
    fun getAllLogs(): Flow<List<DispenseLogEntity>> {
        return dispenseLogDao.getAllLogs()
    }

    fun getAllResults(): Flow<List<ResultEntity>> {
        return resultDao.getAllResults()
    }

    fun getErrorsByAnalysisId(analysisId: Int): Flow<List<ErrorObjectEntity>> {
        return errorObjectDao.getErrorsByAnalysisId(analysisId)
    }

    suspend fun saveNormalTestLog(): Long {
        return dispenseLogDao.insertLog(
            DispenseLogEntity(
                finalResult = "NORMAL",
                errorCount = 0,
                remark = "정상 검증 로그"
            )
        )
    }

    suspend fun saveAnalysisResult(
        finalResult: String,
        summaryText: String?,
        errorObjects: List<ErrorObjectEntity>,
        remark: String? = null
    ): Long {
        val logId = dispenseLogDao.insertLog(
            DispenseLogEntity(
                finalResult = finalResult,
                errorCount = errorObjects.size,
                remark = remark
            )
        )

        val analysisId = resultDao.insertResult(
            ResultEntity(
                resultCode = finalResult,
                summaryText = summaryText
            )
        ).toInt()

        val errorObjectList = errorObjects.map {
            it.copy(analysisId = analysisId)
        }

        if (errorObjectList.isNotEmpty()) {
            errorObjectDao.insertErrorObjects(errorObjectList)
        }

        return logId
    }
}