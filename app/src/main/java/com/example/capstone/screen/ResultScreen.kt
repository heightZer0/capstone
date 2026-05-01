package com.example.capstone.screen
// 결과 화면
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val PrimaryBlue = Color(0xFF2563EB)

// ════════════════════════════════════════════════════════════════
// 오류 결과 화면
// ════════════════════════════════════════════════════════════════
@Composable
fun ErrorResultScreen(
    errorPouchNumbers: List<Int>,
    onRetakeCapture: () -> Unit,
    onGoHome: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {

        // ── 빨간 배너 ─────────────────────────────────────────────
        Surface(
            color    = Color(0xFFDC2626),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier                = Modifier
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                verticalAlignment       = Alignment.CenterVertically,
                horizontalArrangement   = Arrangement.spacedBy(14.dp)
            ) {
                Surface(
                    shape    = CircleShape,
                    color    = Color.White.copy(alpha = 0.22f),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("✕", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Column {
                    Text(
                        "오류 ${errorPouchNumbers.size}건 발견",
                        color      = Color.White,
                        fontSize   = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "약봉지에서 문제가 발견되었습니다",
                        color    = Color.White.copy(alpha = 0.85f),
                        fontSize = 13.sp
                    )
                }
            }
        }

        // ── 오류 봉지 목록 ──────────────────────────────────────────
        LazyColumn(
            modifier        = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding  = PaddingValues(vertical = 16.dp)
        ) {
            items(errorPouchNumbers) { number ->
                ErrorPouchCard(pouchNumber = number)
            }
        }

        // ── 하단 버튼 ─────────────────────────────────────────────
        ResultButtonGroup(
            primaryLabel    = "다시 촬영하기",
            onPrimary       = onRetakeCapture,
            secondaryLabel  = "홈으로 돌아가기",
            onSecondary     = onGoHome
        )
    }
}

@Composable
private fun ErrorPouchCard(pouchNumber: Int) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(containerColor = Color(0xFFF3F4F6)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier         = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text       = "${pouchNumber}번",
                fontSize   = 24.sp,
                fontWeight = FontWeight.Bold,
                color      = Color(0xFF1F2937)
            )
        }
    }
}


// ════════════════════════════════════════════════════════════════
// 정상 결과 화면 (중립 회색 톤 + 검사 소요 시간만 표시)
// ════════════════════════════════════════════════════════════════
@Composable
fun NormalResultScreen(
    elapsedSeconds: Int,
    onRetakeCapture: () -> Unit,
    onGoHome: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {

        // ── 회색(중립) 배너 ────────────────────────────────────────
        Surface(
            color    = Color(0xFF6B7280),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier                = Modifier
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                verticalAlignment       = Alignment.CenterVertically,
                horizontalArrangement   = Arrangement.spacedBy(14.dp)
            ) {
                Surface(
                    shape    = CircleShape,
                    color    = Color.White.copy(alpha = 0.22f),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("✓", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Column {
                    Text(
                        "검사 정상",
                        color      = Color.White,
                        fontSize   = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "모든 약봉지가 정상입니다",
                        color    = Color.White.copy(alpha = 0.85f),
                        fontSize = 13.sp
                    )
                }
            }
        }

        // ── 소요 시간 카드 ─────────────────────────────────────────
        Card(
            modifier  = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape     = RoundedCornerShape(14.dp),
            colors    = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("검사 정보", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("검사 소요 시간", color = Color.Gray, fontSize = 15.sp)
                    Text(
                        "${elapsedSeconds}초",
                        fontWeight = FontWeight.Medium,
                        fontSize   = 15.sp
                    )
                }
            }
        }

        Spacer(Modifier.weight(1f))

        ResultButtonGroup(
            primaryLabel    = "다시 촬영하기",
            onPrimary       = onRetakeCapture,
            secondaryLabel  = "홈으로 돌아가기",
            onSecondary     = onGoHome
        )
    }
}


// ════════════════════════════════════════════════════════════════
// 공통 하단 버튼 영역
// ════════════════════════════════════════════════════════════════
@Composable
private fun ResultButtonGroup(
    primaryLabel: String,
    onPrimary: () -> Unit,
    secondaryLabel: String,
    onSecondary: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick  = onPrimary,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape    = RoundedCornerShape(14.dp),
            colors   = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
        ) {
            Text("📷  $primaryLabel", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }

        OutlinedButton(
            onClick  = onSecondary,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape    = RoundedCornerShape(14.dp)
        ) {
            Text("🏠  $secondaryLabel", fontSize = 15.sp)
        }
    }
}