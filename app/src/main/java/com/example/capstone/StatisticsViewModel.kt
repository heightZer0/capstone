package com.example.capstone

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.capstone.data.local.database.DatabaseProvider
import com.example.capstone.data.local.entity.ErrorObjectEntity
import com.example.capstone.data.local.entity.ResultEntity
import com.example.capstone.screen.InspectionRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class StatisticsViewModel(application: Application) : AndroidViewModel(application) {

    private val db = DatabaseProvider.getDatabase(application)
    private val zone = ZoneId.systemDefault()
    private val timeFmt = DateTimeFormatter.ofPattern("HH:mm")

    // 캘린더에서 점(dot) 표시할 날짜 목록
    val availableDates: Flow<Set<LocalDate>> = db.resultDao().getAllResults()
        .map { entities ->
            entities.map { Instant.ofEpochMilli(it.analyzedTime).atZone(zone).toLocalDate() }.toSet()
        }

    // 특정 검사 상세 (by analysisId)
    fun getResultById(analysisId: Int): Flow<ResultEntity?> = flow {
        emit(db.resultDao().getResultById(analysisId))
    }

    fun getErrorsForAnalysis(analysisId: Int): Flow<List<ErrorObjectEntity>> =
        db.errorObjectDao().getErrorsByAnalysisId(analysisId)

    // 특정 날짜의 검사 목록
    fun getRecordsForDate(date: LocalDate): Flow<List<InspectionRecord>> {
        val start = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val end   = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        return db.resultDao().getResultsByDate(start, end)
            .map { entities ->
                entities.mapIndexed { index, e ->
                    val ldt = Instant.ofEpochMilli(e.analyzedTime).atZone(zone).toLocalDateTime()
                    InspectionRecord(
                        id         = e.analysisId,
                        sequence   = index + 1,
                        date       = date,
                        time       = ldt.format(timeFmt),
                        totalCount = e.totalPouches,
                        errorCount = e.errorCount
                    )
                }
            }
    }
}
