package com.example.capstone

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.capstone.ui.theme.CapstoneTheme
import android.util.Log
import androidx.lifecycle.lifecycleScope
import com.example.capstone.data.local.database.DatabaseProvider
import com.example.capstone.data.local.entity.DispenseLogEntity
import kotlinx.coroutines.launch

private sealed class AppScreen {
    object Home : AppScreen()
    object Statistics : AppScreen()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 상태바 숨기기 (API 무관, AndroidX 방식)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.statusBars())
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        setContent {
            CapstoneTheme {
                AppNavigation()
            }
        }

        val database = DatabaseProvider.getDatabase(this)

        lifecycleScope.launch {
            val logId = database.dispenseLogDao().insertLog(
                DispenseLogEntity(
                    finalResult = "NORMAL",
                    errorCount = 0,
                    remark = "Room DB 테스트 저장"
                )
            )

            Log.e("RoomTest", "저장된 logId: $logId")
        }
    }
}