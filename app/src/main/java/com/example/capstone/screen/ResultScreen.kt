package com.example.capstone.screen
// 결과 화면
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.capstone.InspectionResult
import com.example.capstone.PouchResult

private val PrimaryBlue   = Color(0xFF2563EB)
private val ErrorRed      = Color(0xFFDC2626)
private val NormalGreen   = Color(0xFF22C55E)
private val PageBg        = Color(0xFFF3F4F6)
private val TextPrimary   = Color(0xFF111827)
private val TextSecondary = Color(0xFF6B7280)

// ════════════════════════════════════════════════════════════════
// 오류 결과 화면
// ════════════════════════════════════════════════════════════════
@Composable
fun ErrorResultScreen(
    result: InspectionResult,
    onRetakeCapture: () -> Unit,
    onGoHome: () -> Unit,
    onWatchVideo: (() -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PageBg)
    ) {
        LazyColumn(
            modifier       = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 210.dp)
        ) {
            // ── 빨간 배너 ─────────────────────────────────────────
            item {
                Surface(color = ErrorRed, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier              = Modifier
                            .statusBarsPadding()
                            .padding(horizontal = 20.dp, vertical = 18.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.22f),
                                modifier = Modifier.size(44.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("✕", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Column {
                            Text("오류 ${result.errorPouchNumbers.size}건 발견",
                                color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text("약봉지에서 문제가 발견되었습니다",
                                color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp)
                        }
                    }
                }
            }

            // ── 대표 썸네일 ───────────────────────────────────────
            item { ThumbnailCard(result.thumbnailCrop, modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) }

            // ── 요약 카드 ─────────────────────────────────────────
            item { SummaryCard(result, modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) }

            // ── 봉지별 결과 헤더 ──────────────────────────────────
            if (result.pouches.isNotEmpty()) {
                item {
                    Text(
                        "봉지별 결과 (${result.pouches.size}개)",
                        fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                        color = TextSecondary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
                itemsIndexed(result.pouches) { index, pouch ->
                    PouchRow(pouch = pouch, displayIndex = index + 1, cropBase64 = result.errorCrops[pouch.pouchId])
                }
            } else if (result.errorPouchNumbers.isNotEmpty()) {
                item {
                    Text("오류 봉지", fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                        color = TextSecondary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
                }
                itemsIndexed(result.errorPouchNumbers) { index, pouchId ->
                    PouchRow(pouch = PouchResult(pouchId, 0, null, true), displayIndex = index + 1, cropBase64 = null)
                }
            }
        }

        // ── 하단 버튼 (고정) ──────────────────────────────────────
        Surface(
            modifier  = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
            color     = Color.White,
            shadowElevation = 8.dp
        ) {
            ResultButtonGroup(
                primaryLabel   = "다시 촬영하기",
                onPrimary      = onRetakeCapture,
                secondaryLabel = "홈으로 돌아가기",
                onSecondary    = onGoHome,
                onWatchVideo   = onWatchVideo
            )
        }
    }
}

// ════════════════════════════════════════════════════════════════
// 정상 결과 화면
// ════════════════════════════════════════════════════════════════
@Composable
fun NormalResultScreen(
    result: InspectionResult,
    onRetakeCapture: () -> Unit,
    onGoHome: () -> Unit,
    onWatchVideo: (() -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PageBg)
    ) {
        LazyColumn(
            modifier       = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 210.dp)
        ) {
            // ── 초록 배너 ──────────────────────────────────────────
            item {
                Surface(color = NormalGreen, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier              = Modifier
                            .statusBarsPadding()
                            .padding(horizontal = 20.dp, vertical = 18.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.22f),
                                modifier = Modifier.size(44.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("✓", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Column {
                            Text("검사 정상", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text("모든 약봉지가 정상입니다",
                                color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp)
                        }
                    }
                }
            }

            // ── 대표 썸네일 ───────────────────────────────────────
            item { ThumbnailCard(result.thumbnailCrop, modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) }

            // ── 요약 카드 ─────────────────────────────────────────
            item { SummaryCard(result, modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) }

            // ── 봉지별 결과 ───────────────────────────────────────
            if (result.pouches.isNotEmpty()) {
                item {
                    Text(
                        "봉지별 결과 (${result.pouches.size}개)",
                        fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                        color = TextSecondary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
                itemsIndexed(result.pouches) { index, pouch ->
                    PouchRow(pouch = pouch, displayIndex = index + 1, cropBase64 = null)
                }
            }
        }

        // ── 하단 버튼 (고정) ──────────────────────────────────────
        Surface(
            modifier  = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
            color     = Color.White,
            shadowElevation = 8.dp
        ) {
            ResultButtonGroup(
                primaryLabel   = "다시 촬영하기",
                onPrimary      = onRetakeCapture,
                secondaryLabel = "홈으로 돌아가기",
                onSecondary    = onGoHome,
                onWatchVideo   = onWatchVideo
            )
        }
    }
}

// ════════════════════════════════════════════════════════════════
// 요약 카드
// ════════════════════════════════════════════════════════════════
@Composable
private fun SummaryCard(result: InspectionResult, modifier: Modifier = Modifier) {
    Card(
        modifier  = modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier            = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("검사 요약", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            HorizontalDivider()
            if (result.totalPouches > 0)
                InfoRow("전체 봉지 수", "${result.totalPouches}개")
            if (result.elapsedSeconds > 0)
                InfoRow("소요 시간", "${result.elapsedSeconds}초")
            if (result.errorPouchNumbers.isNotEmpty())
                InfoRow("오류 봉지", "${result.errorPouchNumbers.size}개",
                    valueColor = ErrorRed)
            if (result.pattern.isNotEmpty())
                InfoRow("복약 패턴", patternDesc(result.pattern))
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String, valueColor: Color = TextPrimary) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 14.sp, color = TextSecondary)
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = valueColor)
    }
}

private fun patternDesc(pattern: List<Int>): String = when (pattern.size) {
    1    -> "하루 1회 (${pattern[0]}알씩)"
    2    -> "하루 2회 (${pattern[0]}알 · ${pattern[1]}알)"
    3    -> "하루 3회 (${pattern[0]}알 · ${pattern[1]}알 · ${pattern[2]}알)"
    else -> pattern.joinToString(" · ") { "${it}알" }
}

// ════════════════════════════════════════════════════════════════
// 봉지 한 줄 — 정상이면 컴팩트, 오류면 사진+상세
// ════════════════════════════════════════════════════════════════
@Composable
private fun PouchRow(pouch: PouchResult, displayIndex: Int, cropBase64: String?) {
    if (pouch.error) {
        // 오류 봉지: 사진 + 수치
        Card(
            modifier  = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            shape     = RoundedCornerShape(12.dp),
            colors    = CardDefaults.cardColors(containerColor = Color(0xFFFFF5F5)),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier              = Modifier.fillMaxWidth().padding(12.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (cropBase64 != null) {
                    val bitmap = remember(cropBase64) {
                        runCatching {
                            val bytes = Base64.decode(cropBase64, Base64.DEFAULT)
                            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                        }.getOrNull()
                    }
                    var showFullscreen by remember { mutableStateOf(false) }
                    if (showFullscreen && bitmap != null) {
                        FullscreenImageDialog(bitmap = bitmap, onDismiss = { showFullscreen = false })
                    }
                    if (bitmap != null) {
                        Image(
                            bitmap             = bitmap,
                            contentDescription = "${displayIndex}번 봉지",
                            contentScale       = ContentScale.Crop,
                            modifier           = Modifier
                                .size(72.dp)
                                .background(Color.LightGray, RoundedCornerShape(8.dp))
                                .clickable { showFullscreen = true }
                        )
                    } else {
                        PhotoPlaceholder()
                    }
                } else {
                    PhotoPlaceholder()
                }

                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("${displayIndex}번 봉지",
                            fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                        Surface(shape = RoundedCornerShape(4.dp), color = ErrorRed) {
                            Text("오류", color = Color.White, fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp))
                        }
                    }
                    Text("실제: ${pouch.count}알", fontSize = 14.sp, color = ErrorRed,
                        fontWeight = FontWeight.SemiBold)
                    if (pouch.expected != null)
                        Text("기대: ${pouch.expected}알", fontSize = 13.sp, color = TextSecondary)
                }
            }
        }
    } else {
        // 정상 봉지: 한 줄 컴팩트
        Card(
            modifier  = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp),
            shape     = RoundedCornerShape(10.dp),
            colors    = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Row(
                modifier              = Modifier.fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text("${displayIndex}번 봉지", fontSize = 14.sp, color = TextPrimary)
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("${pouch.count}알", fontSize = 14.sp, color = NormalGreen,
                        fontWeight = FontWeight.Medium)
                    Text("✓", fontSize = 13.sp, color = NormalGreen)
                }
            }
        }
    }
}

@Composable
private fun FullscreenImageDialog(bitmap: ImageBitmap, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            Image(
                bitmap             = bitmap,
                contentDescription = null,
                contentScale       = ContentScale.Fit,
                modifier           = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun ThumbnailCard(thumbnailCrop: String?, modifier: Modifier = Modifier) {
    val bitmap = remember(thumbnailCrop) {
        thumbnailCrop?.let {
            runCatching {
                val bytes = Base64.decode(it, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
            }.getOrNull()
        }
    }
    var showFullscreen by remember { mutableStateOf(false) }
    if (showFullscreen && bitmap != null) {
        FullscreenImageDialog(bitmap = bitmap, onDismiss = { showFullscreen = false })
    }
    Card(
        modifier  = modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        if (bitmap != null) {
            Image(
                bitmap             = bitmap,
                contentDescription = "약봉지 이미지",
                contentScale       = ContentScale.Crop,
                modifier           = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clickable { showFullscreen = true }
            )
        } else {
            Box(
                modifier         = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(Color(0xFFE5E7EB)),
                contentAlignment = Alignment.Center
            ) {
                Text("약봉지 이미지", fontSize = 14.sp, color = TextSecondary)
            }
        }
    }
}

@Composable
private fun PhotoPlaceholder() {
    Box(
        modifier         = Modifier
            .size(72.dp)
            .background(Color(0xFFE5E7EB), RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text("사진\n없음", fontSize = 11.sp, color = TextSecondary)
    }
}

// ════════════════════════════════════════════════════════════════
// 공통 하단 버튼
// ════════════════════════════════════════════════════════════════
@Composable
private fun ResultButtonGroup(
    primaryLabel: String,
    onPrimary: () -> Unit,
    secondaryLabel: String,
    onSecondary: () -> Unit,
    onWatchVideo: (() -> Unit)? = null
) {
    Column(
        modifier            = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (onWatchVideo != null) {
            Button(
                onClick  = onWatchVideo,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape    = RoundedCornerShape(14.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED))
            ) {
                Text("결과 영상 보기", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        Button(
            onClick  = onPrimary,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape    = RoundedCornerShape(14.dp),
            colors   = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
        ) {
            Text(primaryLabel, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
        OutlinedButton(
            onClick  = onSecondary,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape    = RoundedCornerShape(14.dp)
        ) {
            Text(secondaryLabel, fontSize = 15.sp)
        }
    }
}
