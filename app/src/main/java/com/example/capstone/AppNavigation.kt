package com.example.capstone
// 앱 전체 화면 흐름 - 네비게이션 라우터
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.capstone.screen.*
import com.example.capstone.StatisticsViewModel

// ── 라우트 상수 ───────────────────────────────────────────────────
object Routes {
    const val SPLASH      = "splash"
    const val ONBOARDING  = "onboarding"
    const val HOME        = "home"
    const val STATISTICS  = "statistics"
    const val DETAIL      = "detail"
    const val CAMERA      = "camera"
    const val LOADING     = "loading"
    const val RESULT      = "result"
}

// ── NavHost ───────────────────────────────────────────────────────
@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    // Camera → Loading → Result 사이에 공유되는 ViewModel
    // (NavGraph 스코프이므로 세 화면 모두 같은 인스턴스를 사용)
    val inspectionVm: InspectionSharedViewModel = viewModel()

    NavHost(
        navController  = navController,
        startDestination = Routes.SPLASH
    ) {
        // ── Splash ──────────────────────────────────────────────
        composable(Routes.SPLASH) {
            SplashScreen(
                onTimeout = {
                    navController.navigate(Routes.ONBOARDING) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                }
            )
        }

        // ── Onboarding ──────────────────────────────────────────
        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                onFinish = {
                    if (!navController.popBackStack()) {
                        navController.navigate(Routes.HOME)
                    }
                }
            )
        }

        // ── Home ────────────────────────────────────────────────
        composable(Routes.HOME) {
            HomeScreen(
                sharedVm          = inspectionVm,
                onStartClick      = {
                    inspectionVm.resetAnalysis()
                    navController.navigate(Routes.CAMERA)
                },
                onStatisticsClick = {
                    navController.navigate(Routes.STATISTICS)
                },
                onHelpClick = {
                    navController.navigate(Routes.ONBOARDING)
                },
                onInspectionItemClick = { id ->
                    navController.navigate("${Routes.DETAIL}/$id")
                }
            )
        }

        composable(Routes.STATISTICS) {
            val statsVm: StatisticsViewModel = viewModel()
            StatisticsRootScreen(
                vm     = statsVm,
                onBack = { navController.popBackStack() },
                onInspectionItemClick = { record ->
                    navController.navigate("${Routes.DETAIL}/${record.id}")
                }
            )
        }

        composable("${Routes.DETAIL}/{analysisId}") { backStackEntry ->
            val analysisId = backStackEntry.arguments?.getString("analysisId")?.toIntOrNull() ?: 0
            val statsVm: StatisticsViewModel = viewModel()
            InspectionDetailScreen(
                analysisId = analysisId,
                vm         = statsVm,
                onBack     = { navController.popBackStack() }
            )
        }

        // ── Camera ──────────────────────────────────────────────
        composable(Routes.CAMERA) {
            CameraScreen(
                sharedVm      = inspectionVm,
                onStopCapture = {
                    navController.navigate(Routes.LOADING)
                },
                onClose       = {
                    navController.popBackStack(Routes.HOME, inclusive = false)
                }
            )
        }

        // ── Loading (분석 대기) ──────────────────────────────────
        composable(Routes.LOADING) {
            LoadingScreen(
                sharedVm           = inspectionVm,
                onAnalysisComplete = {
                    navController.navigate(Routes.RESULT) {
                        popUpTo(Routes.CAMERA) { inclusive = true }
                    }
                }
            )
        }

        // ── Result ──────────────────────────────────────────────
        composable(Routes.RESULT) {
            val results = inspectionVm.inspectionResults
            if (results.isEmpty()) {
                HomeScreen(
                    onStartClick      = { navController.navigate(Routes.CAMERA) },
                    onStatisticsClick = {}
                )
            } else {
                BatchResultScreen(
                    results         = results,
                    onRetakeCapture = {
                        inspectionVm.resetAnalysis()
                        navController.navigate(Routes.CAMERA) {
                            popUpTo(Routes.HOME) { inclusive = false }
                        }
                    },
                    onGoHome        = {
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.HOME) { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}