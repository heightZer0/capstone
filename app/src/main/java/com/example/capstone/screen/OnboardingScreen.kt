package com.example.capstone.screen
// 튜토리얼 화면
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.capstone.ui.theme.BrandBlue
import com.example.capstone.ui.theme.SurfaceGray
import com.example.capstone.ui.theme.TextPrimary
import com.example.capstone.ui.theme.TextSecondary
import kotlinx.coroutines.launch

data class OnboardingPage(
    val icon        : ImageVector,
    val title       : String,
    val description : String,
    val hint        : String
)

private val pages = listOf(
    OnboardingPage(Icons.Filled.CameraAlt,   "검사 시작하기", "검사 시작하기 버튼을 눌러\n약 봉지 검사를 시작하세요",          "💡 하단의 파란색 버튼을 눌러주세요"),
    OnboardingPage(Icons.Filled.CropFree,    "촬영 가이드",  "가이드 라인 안에 약 봉지가 들어오\n도록 맞춰주세요",              "💡 화면을 가로로 돌려 촬영합니다"),
    OnboardingPage(Icons.Filled.CheckCircle, "결과 확인",   "검사 후 오류 여부와 봉투 번호를\n확인할 수 있습니다",             "💡 오류 발견 시 빨간색으로 표시됩니다")
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope      = rememberCoroutineScope()
    val isLast     = pagerState.currentPage == pages.lastIndex

    Box(modifier = Modifier.fillMaxSize().background(SurfaceGray)) {
        Column(modifier = Modifier.fillMaxSize()) {

            // 건너뛰기
            Box(
                modifier         = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 8.dp, vertical = 4.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                TextButton(onClick = onFinish) {
                    Text("건너뛰기", color = TextSecondary, fontSize = 14.sp)
                }
            }

            // 페이저
            HorizontalPager(
                state    = pagerState,
                modifier = Modifier.weight(1f).fillMaxWidth()
            ) { page ->
                Column(
                    modifier            = Modifier.fillMaxSize().padding(horizontal = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Surface(
                        modifier = Modifier.size(100.dp),
                        shape    = RoundedCornerShape(22.dp),
                        color    = BrandBlue
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector        = pages[page].icon,
                                contentDescription = pages[page].title,
                                tint               = Color.White,
                                modifier           = Modifier.size(52.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(32.dp))
                    Text(pages[page].title, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(12.dp))
                    Text(pages[page].description, fontSize = 15.sp, color = TextSecondary, textAlign = TextAlign.Center, lineHeight = 22.sp)
                    Spacer(Modifier.height(24.dp))
                    Surface(shape = RoundedCornerShape(20.dp), color = BrandBlue.copy(alpha = 0.08f)) {
                        Text(pages[page].hint, fontSize = 13.sp, color = BrandBlue, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                    }
                }
            }

            // 하단 인디케이터 + 버튼
            Column(
                modifier            = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    repeat(pages.size) { index ->
                        val isSelected = index == pagerState.currentPage
                        val dotColor by animateColorAsState(
                            targetValue   = if (isSelected) BrandBlue else Color(0xFFD1D5DB),
                            animationSpec = tween(250),
                            label         = "dot_$index"
                        )
                        Box(
                            modifier = Modifier
                                .width(if (isSelected) 24.dp else 8.dp)
                                .height(8.dp)
                                .background(dotColor, CircleShape)
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = {
                        if (isLast) onFinish()
                        else scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape    = RoundedCornerShape(10.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = BrandBlue)
                ) {
                    Text(if (isLast) "시작하기" else "다음", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                }
            }
        }
    }
}