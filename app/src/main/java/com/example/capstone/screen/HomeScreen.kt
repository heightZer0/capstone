package com.example.capstone.screen
// 홈 화면
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.style.TextOverflow
import com.example.capstone.InspectionSharedViewModel

// ────────────────────────────────────────────────────────────────
// 1. Data Layer  ← 나중에 DB/Repository로 교체할 부분
// ────────────────────────────────────────────────────────────────

enum class InspectionStatus { NORMAL, ERROR }

data class RecentInspectionItem(
    val id: String,
    val dateTime: String,
    val status: InspectionStatus,
    val errorDetail: String? = null   // 오류 시 "48번 약봉지 오류" 등
)

// 더미 데이터 — DB 연결 시 이 리스트만 ViewModel의 StateFlow로 교체
val dummyRecentInspections = listOf(
    RecentInspectionItem(
        id = "1",
        dateTime = "2026-03-27 14:30",
        status = InspectionStatus.ERROR,
        errorDetail = "48번 약봉지 오류"
    ),
    RecentInspectionItem(
        id = "2",
        dateTime = "2026-03-27 13:15",
        status = InspectionStatus.NORMAL
    ),
    RecentInspectionItem(
        id = "3",
        dateTime = "2026-03-27 11:45",
        status = InspectionStatus.NORMAL
    )
)

// ────────────────────────────────────────────────────────────────
// 2. 색상 상수
// ────────────────────────────────────────────────────────────────

private val PrimaryBlue   = Color(0xFF2563EB)
private val ErrorRed      = Color(0xFFEF4444)
private val NormalGreen   = Color(0xFF22C55E)
private val CardBg        = Color(0xFFFFFFFF)
private val PageBg        = Color(0xFFF3F4F6)
private val TextPrimary   = Color(0xFF111827)
private val TextSecondary = Color(0xFF6B7280)

// ────────────────────────────────────────────────────────────────
// 3. HomeScreen  ← 진입점 (네비게이션 연결 시 람다만 채워주면 됨)
// ────────────────────────────────────────────────────────────────

@Composable
fun HomeScreen(
    sharedVm: InspectionSharedViewModel? = null,
    onStartClick: () -> Unit = {},          // → CameraScreen
    onStatisticsClick: () -> Unit = {},
    onHelpClick: () -> Unit = {},
    onInspectionItemClick: (Int) -> Unit = {},
    recentInspections: List<RecentInspectionItem> = dummyRecentInspections  // ← DB 교체 포인트
) {
    // ── DB에서 최근 검사 결과 ─────────────────────────────────────
    val recentInspections by (sharedVm?.recentResults ?: kotlinx.coroutines.flow.flowOf(dummyRecentInspections))
        .collectAsState(initial = dummyRecentInspections)

    // ── 서버 URL 설정 다이얼로그 ──────────────────────────────────
    var showUrlDialog by remember { mutableStateOf(false) }
    var urlInput      by remember { mutableStateOf(sharedVm?.serverUrl ?: "") }

    if (showUrlDialog) {
        AlertDialog(
            onDismissRequest = { showUrlDialog = false },
            title = { Text("서버 URL 설정") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Colab에서 발급받은 ngrok URL을 입력하세요.", fontSize = 13.sp, color = TextSecondary)
                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = { urlInput = it },
                        placeholder = { Text("https://xxxx.ngrok-free.app") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    sharedVm?.serverUrl = urlInput.trim()
                    showUrlDialog = false
                }) { Text("저장") }
            },
            dismissButton = {
                TextButton(onClick = { showUrlDialog = false }) { Text("취소") }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(PageBg),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // 헤더
        item { HomeHeader(onHelpClick = onHelpClick, onSettingsClick = { showUrlDialog = true }) }

        // 본문 패딩 영역
        item {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(modifier = Modifier.height(4.dp))
                GuideCard()
                // 서버 URL 상태 카드
                if (sharedVm != null) {
                    ServerUrlCard(
                        url = sharedVm.serverUrl,
                        onChangeClick = { urlInput = sharedVm.serverUrl; showUrlDialog = true }
                    )
                }
                ActionButtons(
                    onStartClick = onStartClick,
                    onStatisticsClick = onStatisticsClick
                )
            }
        }

        // 최근 검사 결과 섹션 헤더
        item {
            RecentResultsHeader(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )
        }

        // 최근 검사 결과 리스트 (DB 연동)
        items(
            items = recentInspections,
            key = { it.id }
        ) { item ->
            RecentInspectionRow(
                item = item,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                onClick = { onInspectionItemClick(item.id.toInt()) }
            )
        }
    }
}

// ────────────────────────────────────────────────────────────────
// 4. 헤더 영역
// ────────────────────────────────────────────────────────────────

@Composable
private fun HomeHeader(onHelpClick: () -> Unit = {}, onSettingsClick: () -> Unit = {}) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(PrimaryBlue)
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MedicalServices,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = "약봉지 검수 시스템",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "스마트폰 기반 검사",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 13.sp
                )
            }
        }

        Row(modifier = Modifier.align(Alignment.CenterEnd)) {
            IconButton(onClick = onSettingsClick) {
                Icon(Icons.Default.Settings, contentDescription = "서버 설정", tint = Color.White)
            }
            IconButton(onClick = onHelpClick) {
                Icon(Icons.Default.HelpOutline, contentDescription = "도움말", tint = Color.White)
            }
        }
    }
}

// ── 서버 URL 상태 카드 ─────────────────────────────────────────────
@Composable
private fun ServerUrlCard(url: String, onChangeClick: () -> Unit) {
    val isConnected = url.isNotBlank()
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(if (isConnected) NormalGreen else ErrorRed)
                )
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("서버 연결", fontSize = 13.sp, color = TextSecondary)
                    Text(
                        text = if (isConnected) url else "미설정",
                        fontSize = 13.sp,
                        color = if (isConnected) TextPrimary else ErrorRed,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            TextButton(onClick = onChangeClick) {
                Text("변경", fontSize = 13.sp, color = PrimaryBlue)
            }
        }
    }
}

// ────────────────────────────────────────────────────────────────
// 5. 사용 방법 카드
// ────────────────────────────────────────────────────────────────

@Composable
private fun GuideCard() {
    val steps = listOf(
        "약봉지를 카메라에 맞춰 촬영합니다",
        "자동으로 분석이 진행됩니다",
        "결과에서 오류 여부를 확인합니다"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = PrimaryBlue,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "사용 방법",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            steps.forEachIndexed { index, step ->
                Row(verticalAlignment = Alignment.Top) {
                    Text(
                        text = "${index + 1}.",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryBlue,
                        modifier = Modifier.width(22.dp)
                    )
                    Text(
                        text = step,
                        fontSize = 14.sp,
                        color = TextPrimary,
                        lineHeight = 20.sp
                    )
                }
                if (index < steps.lastIndex) Spacer(modifier = Modifier.height(6.dp))
            }
        }
    }
}

// ────────────────────────────────────────────────────────────────
// 6. 버튼 영역
// ────────────────────────────────────────────────────────────────

@Composable
private fun ActionButtons(
    onStartClick: () -> Unit,
    onStatisticsClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // 검사 시작하기 (Primary)
        Button(
            onClick = onStartClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
        ) {
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "검사 시작하기",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        // 오류 통계 보기 (Outlined)
        OutlinedButton(
            onClick = onStatisticsClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
        ) {
            Icon(
                imageVector = Icons.Default.BarChart,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "오류 통계 보기",
                fontSize = 15.sp
            )
        }
    }
}

// ────────────────────────────────────────────────────────────────
// 7. 최근 결과 섹션 헤더
// ────────────────────────────────────────────────────────────────

@Composable
private fun RecentResultsHeader(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Schedule,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "최근 검사 결과",
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
    }
}

// ────────────────────────────────────────────────────────────────
// 8. 최근 검사 결과 행
// ────────────────────────────────────────────────────────────────

@Composable
private fun RecentInspectionRow(
    item: RecentInspectionItem,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val isError     = item.status == InspectionStatus.ERROR
    val statusColor = if (isError) ErrorRed else NormalGreen
    val statusText  = if (isError) item.errorDetail ?: "오류 발견" else "검사 정상"

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.dateTime,
                    fontSize = 12.sp,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = statusText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = statusColor
                )
            }

            // 상태 점
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(statusColor)
            )
        }
    }
}

// ────────────────────────────────────────────────────────────────
// 9. Preview
// ────────────────────────────────────────────────────────────────

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun HomeScreenPreview() {
    MaterialTheme {
        HomeScreen()
    }
}