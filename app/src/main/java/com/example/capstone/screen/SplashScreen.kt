package com.example.capstone.screen
// 앱 시작 화면(스플래시 화면)
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.capstone.ui.theme.BrandBlue
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onTimeout: () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(
        targetValue   = if (visible) 1f else 0f,
        animationSpec = tween(600),
        label         = "splash_fade"
    )

    LaunchedEffect(Unit) {
        visible = true
        delay(2200)
        onTimeout()
    }

    Box(
        modifier         = Modifier.fillMaxSize().background(BrandBlue),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier            = Modifier.fillMaxSize().alpha(alpha),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                modifier = Modifier.size(100.dp),
                shape    = RoundedCornerShape(22.dp),
                color    = Color.White
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector        = Icons.Filled.Medication,
                        contentDescription = "앱 아이콘",
                        tint               = BrandBlue,
                        modifier           = Modifier.size(64.dp)
                    )
                }
            }

            Spacer(Modifier.height(28.dp))

            Text(
                text       = "약봉지 검수 시스템",
                color      = Color.White,
                fontSize   = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign  = TextAlign.Center
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text      = "스마트폰 기반 검사",
                color     = Color.White.copy(alpha = 0.75f),
                fontSize  = 14.sp,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(48.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment     = Alignment.CenterVertically
            ) {
                repeat(3) { index ->
                    Box(
                        modifier = Modifier
                            .size(if (index == 1) 10.dp else 8.dp)
                            .background(
                                color = Color.White.copy(alpha = if (index == 1) 1f else 0.5f),
                                shape = RoundedCornerShape(50)
                            )
                    )
                }
            }
        }
    }
}