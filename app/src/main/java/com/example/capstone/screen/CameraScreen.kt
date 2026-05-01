package com.example.capstone.screen
// 카메라로 약봉지 촬영
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.capstone.InspectionSharedViewModel

private val PrimaryBlue = Color(0xFF2563EB)
private val DarkBg     = Color(0xFF1A1E2E)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(
    sharedVm: InspectionSharedViewModel,
    onStopCapture: () -> Unit,   // 촬영 종료 → Loading 화면
    onClose: () -> Unit          // X 버튼 → 홈 복귀
) {
    val context         = LocalContext.current
    val lifecycleOwner  = LocalLifecycleOwner.current
    val elapsedSeconds  by sharedVm.elapsedSeconds.collectAsState()

    // ── 카메라 권한 ────────────────────────────────────────────────
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPermission = granted }

    // ── 상태 ──────────────────────────────────────────────────────
    var showGuideSheet by remember { mutableStateOf(false) }
    val sheetState     = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // 타이머 시작 & 권한 요청
    LaunchedEffect(Unit) {
        if (!hasCameraPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
        sharedVm.startTimer()
    }

    // MM:SS 포맷
    val formattedTime = remember(elapsedSeconds) {
        "%02d:%02d".format(elapsedSeconds / 60, elapsedSeconds % 60)
    }

    // ── UI ────────────────────────────────────────────────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        // 카메라 프리뷰
        if (hasCameraPermission) {
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).also { previewView ->
                        ProcessCameraProvider.getInstance(ctx).addListener(
                            {
                                runCatching {
                                    val provider = ProcessCameraProvider.getInstance(ctx).get()
                                    provider.unbindAll()
                                    provider.bindToLifecycle(
                                        lifecycleOwner,
                                        CameraSelector.DEFAULT_BACK_CAMERA,
                                        Preview.Builder().build()
                                            .also { it.setSurfaceProvider(previewView.surfaceProvider) }
                                    )
                                }
                            },
                            ContextCompat.getMainExecutor(ctx)
                        )
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("카메라 권한이 필요합니다", color = Color.White, fontSize = 16.sp)
            }
        }

        // ── 상단 바 ────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // X 닫기
            IconButton(onClick = onClose) {
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.45f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("✕", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // 경과 시간
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color.Black.copy(alpha = 0.55f)
            ) {
                Text(
                    text = formattedTime,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }

            // ? 도움말
            IconButton(onClick = { showGuideSheet = true }) {
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.45f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("?", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 가이드 문구 (상단 중앙)
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color.Black.copy(alpha = 0.65f),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 90.dp)
        ) {
            Text(
                "가이드라인 안에 약봉지를 맞춰주세요",
                color = Color.White,
                fontSize = 14.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        // ── 하단 컨트롤 ────────────────────────────────────────────
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color.Black.copy(alpha = 0.65f)
            ) {
                Text(
                    "약봉지 인식 중...",
                    color = Color.White,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            Button(
                onClick = {
                    sharedVm.stopTimerAndGetElapsed().also { elapsed ->
                        sharedVm.startAnalysis(elapsed)
                    }
                    onStopCapture()
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(52.dp)
            ) {
                Text("촬영 종료", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }

    // ── 촬영 가이드 BottomSheet ────────────────────────────────────
    if (showGuideSheet) {
        ModalBottomSheet(
            onDismissRequest = { showGuideSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            CameraGuideContent(onConfirm = { showGuideSheet = false })
        }
    }
}

// ── 촬영 가이드 내용 ─────────────────────────────────────────────
@Composable
private fun CameraGuideContent(onConfirm: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 36.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            "촬영 가이드",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 4.dp)
        )

        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            GuideItem(1, "약봉지가 화면 안에 들어오도록 촬영해주세요.")
            GuideItem(2, "흔들림 없이 촬영해주세요.")
            GuideItem(3, "조명을 확인해주세요.")
        }

        Spacer(Modifier.height(4.dp))

        Button(
            onClick = onConfirm,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
        ) {
            Text("확인", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun GuideItem(number: Int, text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = PrimaryBlue,
            modifier = Modifier.size(30.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    "$number",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
        Text(text, fontSize = 15.sp, modifier = Modifier.weight(1f))
    }
}