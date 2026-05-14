package com.example.capstone
// AI 모델 연결 시 수정할 코드
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

// ─── Repository 인터페이스 ─────────────────────────────────────────
interface InspectionRepository {
    suspend fun analyze(elapsedSeconds: Int): InspectionResult
}

// ─── 더미 Repository ──────────────────────────────────────────────
class DummyInspectionRepository : InspectionRepository {
    override suspend fun analyze(elapsedSeconds: Int): InspectionResult {
        delay(2_500L)
        return InspectionResult(
            isError           = true,
            errorPouchNumbers = listOf(3, 7),
            elapsedSeconds    = elapsedSeconds,
            totalPouches      = 10,
            pattern           = listOf(2, 2, 1),
            pouches           = listOf(
                PouchResult(1, 2, 2, false),
                PouchResult(2, 2, 2, false),
                PouchResult(3, 1, 2, true),
                PouchResult(4, 2, 1, false),
                PouchResult(5, 2, 2, false),
                PouchResult(6, 2, 2, false),
                PouchResult(7, 3, 1, true),
                PouchResult(8, 2, 2, false),
                PouchResult(9, 1, 2, false),
                PouchResult(10, 2, 1, false),
            )
        )
    }
}

// ─── 실제 API Repository ──────────────────────────────────────────
// serverUrl: Colab ngrok URL (예: https://xxxx.ngrok-free.app)
// videoFile: 촬영된 영상 파일
class ApiInspectionRepository(
    private val serverUrl: String,
    private val videoFile: File
) : InspectionRepository {
    override suspend fun analyze(elapsedSeconds: Int): InspectionResult = withContext(Dispatchers.IO) {
        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(300, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .build()

        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "video", videoFile.name,
                videoFile.asRequestBody("video/mp4".toMediaType())
            )
            .build()

        val request = Request.Builder()
            .url("${serverUrl.trimEnd('/')}/analyze")
            .addHeader("ngrok-skip-browser-warning", "true")
            .post(body)
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) throw IOException("서버 오류: ${response.code}")

        val json         = JSONObject(response.body!!.string())
        val errorArray   = json.getJSONArray("errorPouchNumbers")
        val pouchesArray = json.optJSONArray("pouches")
        val patternArray = json.optJSONArray("pattern")
        val cropsObject  = json.optJSONObject("errorCrops")

        val pattern = if (patternArray != null)
            (0 until patternArray.length()).map { patternArray.getInt(it) }
        else emptyList()

        val pouches = if (pouchesArray != null)
            (0 until pouchesArray.length()).map { i ->
                val p = pouchesArray.getJSONObject(i)
                PouchResult(
                    pouchId  = p.getInt("pouchId"),
                    count    = p.getInt("count"),
                    expected = if (p.isNull("expected")) null else p.getInt("expected"),
                    error    = p.getBoolean("error")
                )
            }
        else emptyList()

        val errorCrops: Map<Int, String> = if (cropsObject != null)
            cropsObject.keys().asSequence().associate { key ->
                key.toInt() to cropsObject.getString(key)
            }
        else emptyMap()

        InspectionResult(
            isError           = json.getBoolean("isError"),
            errorPouchNumbers = (0 until errorArray.length()).map { errorArray.getInt(it) },
            elapsedSeconds    = json.getInt("elapsedSeconds"),
            totalPouches      = pouchesArray?.length() ?: 0,
            pattern           = pattern,
            pouches           = pouches,
            videoId           = json.optString("videoId").takeIf { it.isNotBlank() },
            errorCrops        = errorCrops
        )
    }
}
