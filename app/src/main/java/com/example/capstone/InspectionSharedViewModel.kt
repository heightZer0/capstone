package com.example.capstone
// 카메라, 로딩, 결과 화면이 공유하는 상태 관리 코드
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// ─── 공유 ViewModel ───────────────────────────────────────────────
// NavGraph 스코프로 생성되어 Camera → Loading → Result 사이에 상태를 공유
class InspectionSharedViewModel(
    private val repository: InspectionRepository = DummyInspectionRepository()
) : ViewModel() {

    var isResultSavedToDb by mutableStateOf(false)
        private set

    fun markResultSavedToDb() {
        isResultSavedToDb = true
    }

    // 촬영 타이머
    private val _elapsedSeconds = MutableStateFlow(0)
    val elapsedSeconds: StateFlow<Int> = _elapsedSeconds
    private var timerJob: Job? = null

    // 최종 검사 결과
    var inspectionResult: InspectionResult? by mutableStateOf(null)
        private set

    // 분석 완료 여부 (LoadingScreen → ResultScreen 전환 신호)
    var isAnalysisComplete by mutableStateOf(false)
        private set

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
        viewModelScope.launch {
            val result = repository.analyze(elapsedSeconds)
            inspectionResult = result
            isAnalysisComplete = true
        }
    }

    fun resetAnalysis() {
        isAnalysisComplete = false
        inspectionResult = null
        _elapsedSeconds.value = 0
        isResultSavedToDb = false
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}