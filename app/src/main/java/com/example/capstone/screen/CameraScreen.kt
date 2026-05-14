package com.example.capstone.screen
// 카메라로 약봉지 촬영
import android.Manifest
import android.app.Activity
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.camera.view.PreviewView
import java.io.File
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
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

    // ── 녹화 상태 ─────────────────────────────────────────────────
    val recordingRef  = remember { mutableStateOf<Recording?>(null) }
    val outputFile    = remember { File(context.cacheDir, "inspection_${System.currentTimeMillis()}.mp4") }
    val elapsedHolder = remember { intArrayOf(0) }  // stop 시점 elapsed 캡처용
    var isStopping    by remember { mutableStateOf(false) }

    // 녹화 완료 후 화면 전환 (main thread 콜백 → LaunchedEffect 경유)
    var shouldNavigate by remember { mutableStateOf(false) }
    val currentOnStopCapture by rememberUpdatedState(onStopCapture)
    LaunchedEffect(shouldNavigate) {
        if (shouldNavigate) currentOnStopCapture()
    }

    // 가로 모드 고정 (파이프라인이 1920x1080 가로 영상 기준)
    DisposableEffect(Unit) {
        val activity = context as? Activity
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

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
        // 카메라 프리뷰 — 가로 화면 중앙에 1:1 정사각형
        if (hasCameraPermission) {
            AndroidView(
                factory = { ctx ->
                    val executor = ContextCompat.getMainExecutor(ctx)
                    PreviewView(ctx).also { previewView ->
                        previewView.scaleType = PreviewView.ScaleType.FILL_CENTER
                        // VideoCapture 설정
                        val recorder    = Recorder.Builder()
                            .setQualitySelector(QualitySelector.from(Quality.HD))
                            .build()
                        val videoCapture = VideoCapture.withOutput(recorder)

                        ProcessCameraProvider.getInstance(ctx).addListener(
                            {
                                runCatching {
                                    val provider = ProcessCameraProvider.getInstance(ctx).get()
                                    provider.unbindAll()
                                    provider.bindToLifecycle(
                                        lifecycleOwner,
                                        CameraSelector.DEFAULT_BACK_CAMERA,
                                        Preview.Builder().build()
                                            .also { it.setSurfaceProvider(previewView.surfaceProvider) },
                                        videoCapture
                                    )
                                    // 카메라 바인딩 직후 자동 녹화 시작
                                    recordingRef.value = videoCapture.output
                                        .prepareRecording(ctx, FileOutputOptions.Builder(outputFile).build())
                                        .start(executor) { event ->
                                            if (event is VideoRecordEvent.Finalize) {
                                                sharedVm.videoFile = if (!event.hasError()) outputFile else null
                                                sharedVm.startAnalysis(elapsedHolder[0])
                                                shouldNavigate = true
                                            }
                                        }
                                }
                            },
                            executor
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

        // ── ROI 가이드 오버레이 — 1:1 박스와 동일 크기/위치 ──────────
        // ROI_Y_TOP=100, ROI_Y_BOTTOM=620 (1280x720 기준)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val roiTop    = size.height * (100f / 720f)
            val roiBottom = size.height * (620f / 720f)
            val roiHeight = roiBottom - roiTop
            val dimColor  = Color.Black.copy(alpha = 0.55f)

            // 위 어두운 영역
            drawRect(color = dimColor, topLeft = Offset(0f, 0f),
                     size = Size(size.width, roiTop))
            // 아래 어두운 영역
            drawRect(color = dimColor, topLeft = Offset(0f, roiBottom),
                     size = Size(size.width, size.height - roiBottom))
            // ROI 테두리
            drawRect(color = Color.White, topLeft = Offset(0f, roiTop),
                     size = Size(size.width, roiHeight),
                     style = Stroke(width = 3f))
            // 모서리 마커
            val m = 40f
            val t = 6f
            listOf(
                Offset(0f, roiTop) to Offset(m, roiTop),
                Offset(0f, roiTop) to Offset(0f, roiTop + m),
                Offset(size.width - m, roiTop) to Offset(size.width, roiTop),
                Offset(size.width, roiTop) to Offset(size.width, roiTop + m),
                Offset(0f, roiBottom - m) to Offset(0f, roiBottom),
                Offset(0f, roiBottom) to Offset(m, roiBottom),
                Offset(size.width - m, roiBottom) to Offset(size.width, roiBottom),
                Offset(size.width, roiBottom - m) to Offset(size.width, roiBottom),
            ).forEach { (s, e) ->
                drawLine(color = Color(0xFF00E5FF), start = s, end = e, strokeWidth = t)
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

        // 가이드 문구 — ROI 바로 위에 표시
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color.Black.copy(alpha = 0.65f),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxHeight(0.185f)
                .wrapContentHeight(Alignment.Bottom)
                .padding(bottom = 6.dp)
        ) {
            Text(
                "밝은 영역 안에 컨베이어 벨트를 맞춰주세요",
                color = Color.White,
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
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
                    if (!isStopping) {
                        isStopping = true
                        elapsedHolder[0] = sharedVm.stopTimerAndGetElapsed()
                        if (recordingRef.value != null) {
                            // 녹화 중 → stop() 호출, VideoRecordEvent.Finalize에서 화면 전환
                            recordingRef.value?.stop()
                        } else {
                            // 녹화 미시작(권한 없음 등) → 바로 처리
                            sharedVm.startAnalysis(elapsedHolder[0])
                            shouldNavigate = true
                        }
                    }
                },
                enabled = !isStopping,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(52.dp)
            ) {
                Text(
                    if (isStopping) "처리 중..." else "촬영 종료",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
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
