package com.example.capstone.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.capstone.data.local.database.DatabaseProvider
import com.example.capstone.data.local.entity.ErrorObjectEntity
import com.example.capstone.data.repository.DispenseRepository
import kotlinx.coroutines.launch
import android.util.Log

class DispenseViewModel(application: Application) : AndroidViewModel(application) {

    private val database = DatabaseProvider.getDatabase(application)

    private val repository = DispenseRepository(
        dispenseLogDao = database.dispenseLogDao(),
        resultDao = database.resultDao(),
        errorCodeDao = database.errorCodeDao(),
        errorObjectDao = database.errorObjectDao()
    )

    fun saveTestErrorResult() {
        viewModelScope.launch {
            repository.saveAnalysisResult(
                finalResult = "ERROR",
                summaryText = "2개의 오류 포지가 발견되었습니다.",
                errorObjects = listOf(
                    ErrorObjectEntity(
                        analysisId = 0,
                        errorCode = "CNT",
                        pouchNo = 1,
                        errorMessage = "1번 포지의 약 개수가 기준과 다릅니다."
                    ),
                    ErrorObjectEntity(
                        analysisId = 0,
                        errorCode = "CLR",
                        pouchNo = 3,
                        errorMessage = "3번 포지의 색상이 기준과 다릅니다."
                    )
                ),
                remark = "ViewModel을 통한 오류 분석 저장"
            )
        }
    }
    fun saveInspectionResult(
        isError: Boolean,
        errorPouchNumbers: List<Int>,
        elapsedSeconds: Int? = null
    ) {
        viewModelScope.launch {
            if (isError) {
                val logId = repository.saveAnalysisResult(
                    finalResult = "ERROR",
                    summaryText = "${errorPouchNumbers.size}개의 오류 포지가 발견되었습니다.",
                    errorObjects = errorPouchNumbers.map { pouchNo ->
                        ErrorObjectEntity(
                            analysisId = 0,
                            errorCode = "CNT",
                            pouchNo = pouchNo,
                            errorMessage = "${pouchNo}번 포지에서 오류가 발견되었습니다."
                        )
                    },
                    remark = "분석 결과 오류 저장"
                )

                Log.e("RoomTest", "실제 오류 결과 저장 완료 logId: $logId")
            } else {
                val logId = repository.saveAnalysisResult(
                    finalResult = "NORMAL",
                    summaryText = "정상 포지로 판정되었습니다.",
                    errorObjects = emptyList(),
                    remark = "분석 결과 정상 저장, 소요 시간: ${elapsedSeconds ?: 0}초"
                )

                Log.e("RoomTest", "실제 정상 결과 저장 완료 logId: $logId")
            }
        }
    }
}