package com.example.capstone
// AI 모델 연결 시 수정할 코드
import kotlinx.coroutines.delay

// ─── Repository 인터페이스 ─────────────────────────────────────────
// AI 모델/DB 연결 시 이 인터페이스를 구현하는 실제 Repository로 교체
interface InspectionRepository {
    suspend fun analyze(elapsedSeconds: Int): InspectionResult
}

// ─── 더미 Repository ──────────────────────────────────────────────
// 실제 분석 없이 고정된 오류 결과를 반환 (개발/테스트용)
class DummyInspectionRepository : InspectionRepository {
    override suspend fun analyze(elapsedSeconds: Int): InspectionResult {
        delay(2_500L) // 분석 지연 시뮬레이션 (2.5초)
        return InspectionResult(
            isError = true,
            errorPouchNumbers = listOf(48, 23, 15),
            elapsedSeconds = elapsedSeconds
        )
    }
}