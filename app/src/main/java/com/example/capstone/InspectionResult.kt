package com.example.capstone

// ─── 결과 데이터 모델 ────────────────────────────────────────────
data class InspectionResult(
    val isError: Boolean,
    val errorPouchNumbers: List<Int> = emptyList(),   // 오류 약봉지 번호 목록
    val elapsedSeconds: Int = 0                        // 촬영 소요 시간(초)
)