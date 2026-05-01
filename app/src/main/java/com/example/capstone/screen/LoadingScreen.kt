package com.example.capstone.screen
// 영상 분석 대기 화면
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.capstone.InspectionSharedViewModel

private val PrimaryBlue = Color(0xFF2563EB)

@Composable
fun LoadingScreen(
    sharedVm: InspectionSharedViewModel,
    onAnalysisComplete: () -> Unit  // Result 화면으로 전환
) {
    // 분석 완료 감지 → 화면 전환
    val isComplete = sharedVm.isAnalysisComplete
    LaunchedEffect(isComplete) {
        if (isComplete) onAnalysisComplete()
    }

    // ── 분석 단계 상태 ─────────────────────────────────────────────
    // 0.5초 간격으로 단계별 표시 (UX용 연출)
    var step by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(600L);  step = 1
        kotlinx.coroutines.delay(700L);  step = 2
        kotlinx.coroutines.delay(800L);  step = 3
    }

    // ── UI ────────────────────────────────────────────────────────
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(72.dp),
            color = PrimaryBlue,
            strokeWidth = 5.dp
        )

        Spacer(Modifier.height(32.dp))

        Text(
            "영상을 분석하고 있습니다",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(8.dp))

        Text(
            "잠시만 기다려주세요...",
            fontSize = 14.sp,
            color = Color.Gray
        )

        Spacer(Modifier.height(48.dp))

        // 진행 단계 표시
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            LoadingStep(
                label   = "영상 업로드 완료",
                state   = when { step >= 1 -> StepState.DONE; else -> StepState.PENDING }
            )
            LoadingStep(
                label   = "AI 분석 진행 중",
                state   = when { step >= 2 -> StepState.DONE; step == 1 -> StepState.IN_PROGRESS; else -> StepState.PENDING }
            )
            LoadingStep(
                label   = "결과 생성 대기 중",
                state   = when { step >= 3 -> StepState.IN_PROGRESS; else -> StepState.PENDING }
            )
        }
    }
}

// ── 단계 상태 sealed class ────────────────────────────────────────
private enum class StepState { DONE, IN_PROGRESS, PENDING }

@Composable
private fun LoadingStep(label: String, state: StepState) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(Modifier.size(26.dp), contentAlignment = Alignment.Center) {
            when (state) {
                StepState.DONE -> Surface(
                    shape = CircleShape,
                    color = Color(0xFF16A34A),
                    modifier = Modifier.size(26.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("✓", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
                StepState.IN_PROGRESS -> CircularProgressIndicator(
                    modifier    = Modifier.size(22.dp),
                    color       = PrimaryBlue,
                    strokeWidth = 2.5.dp
                )
                StepState.PENDING -> Surface(
                    shape  = CircleShape,
                    color  = Color.LightGray,
                    modifier = Modifier.size(26.dp)
                ) {}
            }
        }

        Text(
            text  = label,
            fontSize = 15.sp,
            color = if (state == StepState.PENDING) Color.Gray else Color.Black
        )
    }
}