package com.example.capstone

// ─── 봉지별 결과 ─────────────────────────────────────────────────
data class PouchResult(
    val pouchId: Int,
    val count: Int,
    val expected: Int?,
    val error: Boolean
)

// ─── 검사 결과 데이터 모델 ────────────────────────────────────────────
data class InspectionResult(
    val isError: Boolean,
    val errorPouchNumbers: List<Int> = emptyList(),   // 오류 약봉지 번호 목록
    val elapsedSeconds: Int = 0,                       // 촬영 소요 시간(초)
    val totalPouches: Int = 0,                         // 전체 봉지 수
    val pattern: List<Int> = emptyList(),              // 감지된 복약 패턴 (예: [2, 2, 1])
    val pouches: List<PouchResult> = emptyList(),      // 봉지별 상세 결과
    val videoId: String? = null,                       // 서버에 저장된 결과 영상 ID
    val errorCrops: Map<Int, String> = emptyMap()      // 오류 봉지 사진 (pouchId -> base64 jpg)
)
