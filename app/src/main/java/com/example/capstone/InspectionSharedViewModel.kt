package com.example.capstone
// 카메라, 로딩, 결과 화면이 공유하는 상태 관리 코드
import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.capstone.data.local.database.DatabaseProvider
import com.example.capstone.data.local.entity.ErrorObjectEntity
import com.example.capstone.data.local.entity.ResultEntity
import com.example.capstone.screen.InspectionStatus
import com.example.capstone.screen.RecentInspectionItem
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ─── 공유 ViewModel ───────────────────────────────────────────────
class InspectionSharedViewModel(application: Application) : AndroidViewModel(application) {

    private val db = DatabaseProvider.getDatabase(application)

    private val prefs = application.getSharedPreferences("settings", android.content.Context.MODE_PRIVATE)

    // 서버 URL (홈 화면에서 설정) — SharedPreferences로 persist
    private var _serverUrl by mutableStateOf(prefs.getString("server_url", "") ?: "")
    var serverUrl: String
        get() = _serverUrl
        set(value) {
            _serverUrl = value
            prefs.edit().putString("server_url", value).apply()
        }

    // 촬영된 영상 파일 (CameraScreen에서 녹화 완료 후 설정)
    var videoFile: File? = null

    // 촬영 타이머
    private val _elapsedSeconds = MutableStateFlow(0)
    val elapsedSeconds: StateFlow<Int> = _elapsedSeconds
    private var timerJob: Job? = null

    // 최종 검사 결과 (배치 목록)
    var inspectionResults: List<InspectionResult> by mutableStateOf(emptyList())
        private set

    // 분석 완료 여부 (LoadingScreen → ResultScreen 전환 신호)
    var isAnalysisComplete by mutableStateOf(false)
        private set

    // DB 저장 완료 여부 (ResultScreen 재컴포지션 시 중복 저장 방지)
    var isResultSavedToDb by mutableStateOf(false)
        private set

    fun markResultSavedToDb() {
        isResultSavedToDb = true
    }

    // 홈 화면 최근 검사 결과 (DB에서 실시간)
    val recentResults: Flow<List<RecentInspectionItem>> = db.resultDao().getRecentResults(10)
        .map { entities ->
            val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            entities.map { e ->
                RecentInspectionItem(
                    id          = e.analysisId.toString(),
                    dateTime    = fmt.format(Date(e.analyzedTime)),
                    status      = if (e.resultCode == "ERROR") InspectionStatus.ERROR else InspectionStatus.NORMAL,
                    errorDetail = if (e.resultCode == "ERROR") "오류 봉지 ${e.errorCount}개" else null
                )
            }
        }

    // ── 타이머 ───────────────────────────────────────────────────
    fun startTimer() {
        timerJob?.cancel()
        _elapsedSeconds.value = 0
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1_000L)
                _elapsedSeconds.value++
            }
        }
    }

    fun stopTimerAndGetElapsed(): Int {
        timerJob?.cancel()
        return _elapsedSeconds.value
    }

    // ── 분석 요청 ─────────────────────────────────────────────────
    fun startAnalysis(elapsedSeconds: Int) {
        isAnalysisComplete = false
        val file = videoFile
        val repo: InspectionRepository =
            if (serverUrl.isNotBlank() && file != null)
                ApiInspectionRepository(serverUrl, file)
            else
                DummyInspectionRepository()

        viewModelScope.launch {
            try {
                val results = repo.analyze(elapsedSeconds)
                inspectionResults = results
                results.forEach { saveResultToDb(it) }
            } catch (e: Exception) {
                val fallback = InspectionResult(
                    isError = true,
                    errorPouchNumbers = emptyList(),
                    elapsedSeconds = elapsedSeconds
                )
                inspectionResults = listOf(fallback)
                saveResultToDb(fallback)
            }
            isAnalysisComplete = true
        }
    }

    private suspend fun saveResultToDb(result: InspectionResult) {
        val analysisId = db.resultDao().insertResult(
            ResultEntity(
                resultCode   = if (result.isError) "ERROR" else "NORMAL",
                totalPouches = result.totalPouches,
                errorCount   = result.errorPouchNumbers.size
            )
        ).toInt()
        android.util.Log.d("DB_SAVE", "저장됨: analysisId=$analysisId totalPouches=${result.totalPouches}")
        if (result.errorPouchNumbers.isNotEmpty()) {
            db.errorObjectDao().insertErrorObjects(
                result.errorPouchNumbers.map { pouchNo ->
                    ErrorObjectEntity(
                        analysisId   = analysisId,
                        errorCode    = "CNT",
                        pouchNo      = pouchNo
                    )
                }
            )
        }
        videoFile?.delete()
    }

    fun resetAnalysis() {
        isAnalysisComplete = false
        inspectionResults = emptyList()
        videoFile = null
        _elapsedSeconds.value = 0
        isResultSavedToDb = false
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
