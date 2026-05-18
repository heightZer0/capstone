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
    suspend fun analyze(elapsedSeconds: Int): List<InspectionResult>
}

// ─── 더미 Repository ──────────────────────────────────────────────
class DummyInspectionRepository : InspectionRepository {
    override suspend fun analyze(elapsedSeconds: Int): List<InspectionResult> {
        delay(2_500L)
        return listOf(
            InspectionResult(
                isError           = false,
                errorPouchNumbers = emptyList(),
                elapsedSeconds    = elapsedSeconds,
                totalPouches      = 5,
                pattern           = listOf(2, 2, 1),
                pouches           = listOf(
                    PouchResult(1, 2, 2, false),
                    PouchResult(2, 2, 2, false),
                    PouchResult(3, 1, 2, false),
                    PouchResult(4, 2, 2, false),
                    PouchResult(5, 1, 1, false),
                )
            ),
            InspectionResult(
                isError           = true,
                errorPouchNumbers = listOf(3, 5),
                elapsedSeconds    = elapsedSeconds,
                totalPouches      = 5,
                pattern           = listOf(2, 2, 1),
                pouches           = listOf(
                    PouchResult(1, 2, 2, false),
                    PouchResult(2, 2, 2, false),
                    PouchResult(3, 1, 2, true),
                    PouchResult(4, 2, 1, false),
                    PouchResult(5, 3, 1, true),
                )
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
    override suspend fun analyze(elapsedSeconds: Int): List<InspectionResult> = withContext(Dispatchers.IO) {
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

        val responseStr = response.body!!.string()
        val json = JSONObject(responseStr)

        // 새 형식: {"batches": [...]} / 예전 형식: {"isError": ..., "pouches": [...]}
        val batchesArray = json.optJSONArray("batches")
        val batchJsonList = if (batchesArray != null) {
            (0 until batchesArray.length()).map { batchesArray.getJSONObject(it) }
        } else {
            listOf(json)  // 예전 형식 → 배치 1개로 처리
        }

        batchJsonList.map { b ->
            val errorArray   = b.getJSONArray("errorPouchNumbers")
            val pouchesArray = b.optJSONArray("pouches")
            val patternArray = b.optJSONArray("pattern")
            val cropsObject  = b.optJSONObject("errorCrops")

            val pattern = if (patternArray != null)
                (0 until patternArray.length()).map { patternArray.getInt(it) }
            else emptyList()

            val pouches = if (pouchesArray != null)
                (0 until pouchesArray.length()).map { j ->
                    val p = pouchesArray.getJSONObject(j)
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
                isError           = b.getBoolean("isError"),
                errorPouchNumbers = (0 until errorArray.length()).map { errorArray.getInt(it) },
                elapsedSeconds    = b.getInt("elapsedSeconds"),
                totalPouches      = pouchesArray?.length() ?: 0,
                pattern           = pattern,
                pouches           = pouches,
                errorCrops        = errorCrops,
                thumbnailCrop     = b.optString("thumbnailCrop").takeIf { it.isNotBlank() }
            )
        }
    }
}
