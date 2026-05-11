# 알약 자동 검수 시스템

> 캡스톤 디자인 | 한성대학교

컨베이어 벨트 위를 지나가는 약 봉지를 카메라로 촬영하여, **알약 수량 오류를 자동으로 감지**하는 시스템입니다.  
AI 파이프라인(Python)과 Android 앱(Kotlin)으로 구성됩니다.

---

## 시스템 구성

```
약사/작업자
    │
    │  카메라로 촬영
    ▼
[Android 앱]
    │
    │  영상 업로드 (POST /analyze)
    ▼
[AI 파이프라인 서버]  ← Google Colab GPU
    │
    │  JSON 결과 반환
    ▼
[Android 앱]
    오류 봉지 번호 표시 / 검수 통계 화면
```

| 컴포넌트 | 기술 | 위치 |
|---------|------|------|
| AI 파이프라인 | Python, RTMDet, mmdetection | `inspection_pipeline/` |
| 서버 | FastAPI, uvicorn, pyngrok | `inspection_pipeline/server.py` |
| Colab 실행 | Jupyter Notebook | `colab_server.ipynb` |
| Android 앱 | Kotlin, Jetpack Compose | `app/` |

---

## 디렉토리 구조

```
capstone/
├── app/                            # Android 앱 (Kotlin + Jetpack Compose)
│   └── src/main/java/com/example/capstone/
│       ├── InspectionRepository.kt # API 연동 포인트 (DummyInspectionRepository)
│       ├── InspectionResult.kt
│       └── screen/                 # 각 화면 Composable
│
├── inspection_pipeline/            # AI 파이프라인 (Python)
│   ├── pipeline.py                 # 메인 파이프라인 (영상 처리 + 봉지 추적 + 패턴 감지)
│   ├── pill_detector.py            # RTMDet 알약 탐지 + CountStabilizer
│   ├── pouch_splitter.py           # 프레임에서 봉지 영역 분리
│   ├── ocr_reader.py               # EasyOCR (봉지 번호 / 복용시간 / 날짜 인식)
│   ├── visualizer.py               # 결과 시각화 (한글 폰트 지원)
│   └── server.py                   # FastAPI 서버 (/health, /analyze)
│
├── fonts/
│   └── NanumGothic.ttf             # 한글 렌더링용 폰트
│
├── model/
│   ├── rtmdet_single_pill_aug_300.py   # 모델 설정 파일
│   └── README.md                       # 가중치 다운로드 안내 (Google Drive)
│
├── colab_server.ipynb              # Google Colab 서버 실행 노트북
├── requirements.txt                # Python 의존성
└── README.md
```

---

## AI 파이프라인 상세

### 처리 흐름

```
영상 프레임
    │
    ▼
[1] pouch_splitter.py
    검은 텍스트 밀도 피크 감지로 봉지 경계 분리
    → 봉지별 crop / 텍스트 영역 / 알약 영역 반환
    │
    ▼
[2] pill_detector.py  (RTMDet-tiny, mAP@50=0.909)
    ROI 전체에서 한 번에 알약 탐지 (score_thr=0.35)
    → 탐지 결과를 x좌표 기준으로 각 봉지에 배분
    │
    ▼
[3] CountStabilizer  (window=15프레임 최빈값)
    프레임마다 카운트가 흔들리는 것을 안정화
    │
    ▼
[4] PouchTracker
    봉지가 오른쪽에서 진입할 때만 새 ID 부여
    잠깐 사라졌다 재탐지돼도 ID 유지 (max_missing=30)
    │
    ▼
[5] 확정 로직 (CONFIRM_DELAY=8)
    봉지가 화면에서 사라진 후 8 detection 프레임 뒤에 최종 확정
    → count_history 최빈값을 최종 알약 수로 기록
    → history 길이 < 13이면 유령 ID로 간주, 무시
    │
    ▼
[6] PatternMonitor  (투표 기반 패턴 감지)
    확정된 봉지 수량을 순서대로 누적
    길이 1~6 패턴을 모두 시도 → 70% 이상 일치하는 패턴 확정
    → 패턴과 다른 봉지 = 오류
    │
    ▼
[7] 최종 패턴 기준 오류 재계산
    초반에 잘못된 패턴으로 오탐이 나도
    영상 종료 후 최종 패턴 기준으로 전체 재계산
```

### 주요 파라미터

| 파라미터 | 값 | 설명 |
|---------|-----|------|
| `SCORE_THR` | 0.35 | 탐지 confidence 임계값 (빛 반사 오탐 방지) |
| `NMS iou_threshold` | 0.40 | 겹친 bbox 제거 임계값 |
| `CountStabilizer.window` | 15 | 카운트 안정화 프레임 수 |
| `PouchTracker.max_dist` | 250 | 봉지 추적 최대 이동 거리 (px) |
| `PouchTracker.max_missing` | 30 | 봉지가 없어도 ID 유지하는 프레임 수 |
| `PouchTracker.entry_zone` | 0.6 | 오른쪽 60% 이상에서 나타나야 신규 봉지 |
| `CONFIRM_DELAY` | 8 | 봉지 확정 대기 detection 프레임 수 |
| `PatternMonitor.max_pattern_len` | 6 | 최대 패턴 길이 |
| `PatternMonitor` 임계값 | 70% | 패턴 확정 최소 일치율 |

### 모델 정보

- **아키텍처**: RTMDet-tiny
- **학습 데이터**: 단일 클래스 (`Pill`) 커스텀 데이터셋
- **성능**: mAP@50 = **0.909** (epoch 273)
- **입력 해상도**: 1920×1080 (640×640 내부 처리)
- **가중치 파일**: Google Drive 보관 (용량 문제로 GitHub 미포함)
  - 다운로드: `model/README.md` 참고

---

## 설치 및 실행

### Google Colab (권장 — GPU 무료)

1. `colab_server.ipynb`를 Google Drive에 업로드하거나 직접 Colab에서 열기
2. **런타임 → 런타임 유형 변경 → GPU (T4)** 선택
3. 아래 파일들을 `MyDrive/pill_project/` 구조로 Drive에 업로드

```
MyDrive/pill_project/
  inspection_pipeline/   ← pipeline.py, server.py 등
  model/
    rtmdet_single_pill_aug_300.py
    best_coco_bbox_mAP_50_epoch_273.pth   ← Drive에서 별도 다운로드
  fonts/
    NanumGothic.ttf
```

4. 노트북 셀을 순서대로 실행 → 마지막 셀에서 ngrok 공개 URL 확인
5. Android 앱 서버 주소 입력란에 해당 URL 입력

### 로컬 실행

```bash
# 1. conda 환경 생성
conda create -n pill python=3.10
conda activate pill

# 2. PyTorch 설치 (CUDA 버전에 맞게)
pip install torch torchvision --index-url https://download.pytorch.org/whl/cu118

# 3. mmdetection 설치
pip install -U openmim
mim install mmengine
mim install "mmcv>=2.0.0"
mim install mmdet

# 4. 나머지 의존성
pip install -r requirements.txt

# 5. 영상 분석 (결과 영상 저장)
cd inspection_pipeline
python pipeline.py --input 영상경로.mp4 --output output.mp4

# 6. API 서버 실행
uvicorn server:app --host 0.0.0.0 --port 8000
```

**환경변수로 모델 경로 지정 (기본값이 로컬 경로):**

```bash
export PILL_CONFIG=/path/to/rtmdet_single_pill_aug_300.py
export PILL_CHECKPOINT=/path/to/best_coco_bbox_mAP_50_epoch_273.pth
```

---

## API 명세

### `GET /health`

서버 상태 확인

**Response**
```json
{ "status": "ok" }
```

---

### `POST /analyze`

영상 파일을 업로드하여 알약 수량 검수 결과 반환

**Request** — `multipart/form-data`

| 필드 | 타입 | 설명 |
|------|------|------|
| `video` | File | 분석할 영상 파일 (mp4, avi 등) |

**Response** — `application/json`

```json
{
  "isError": true,
  "errorPouchNumbers": [3, 7],
  "elapsedSeconds": 42,
  "pattern": [5, 4],
  "pouches": [
    { "pouchId": 1, "count": 5, "expected": 5, "error": false },
    { "pouchId": 2, "count": 4, "expected": 4, "error": false },
    { "pouchId": 3, "count": 3, "expected": 5, "error": true  }
  ]
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| `isError` | bool | 오류 봉지 존재 여부 |
| `errorPouchNumbers` | int[] | 오류가 있는 봉지 ID 목록 |
| `elapsedSeconds` | int | 분석 소요 시간 (초) |
| `pattern` | int[] | 감지된 알약 수 패턴 (예: [5,4] = 5개-4개 반복) |
| `pouches[].pouchId` | int | 봉지 순번 (진입 순서) |
| `pouches[].count` | int | 실제 감지된 알약 수 |
| `pouches[].expected` | int\|null | 패턴 기준 예상 알약 수 |
| `pouches[].error` | bool | 오류 여부 |

---

## Android 앱 API 연동 방법

현재 `InspectionRepository.kt`의 `DummyInspectionRepository`가 고정된 더미 데이터를 반환합니다.  
실제 API와 연동하려면 아래와 같이 구현체를 교체하면 됩니다.

```kotlin
class ApiInspectionRepository(
    private val serverUrl: String,
    private val videoUri: Uri,
    private val context: Context
) : InspectionRepository {

    override suspend fun analyze(elapsedSeconds: Int): InspectionResult {
        val file = uriToFile(videoUri, context)
        val requestBody = file.asRequestBody("video/mp4".toMediaType())
        val multipart = MultipartBody.Part.createFormData("video", file.name, requestBody)

        val response = OkHttpClient().newCall(
            Request.Builder()
                .url("$serverUrl/analyze")
                .post(MultipartBody.Builder().setType(MultipartBody.FORM)
                    .addPart(multipart).build())
                .build()
        ).execute()

        val json = JSONObject(response.body!!.string())
        return InspectionResult(
            isError            = json.getBoolean("isError"),
            errorPouchNumbers  = json.getJSONArray("errorPouchNumbers")
                                     .let { arr -> (0 until arr.length()).map { arr.getInt(it) } },
            elapsedSeconds     = json.getInt("elapsedSeconds")
        )
    }
}
```

---

## 성능

| 항목 | 수치 |
|------|------|
| 알약 탐지 mAP@50 | 0.909 |
| 봉지 수량 검수 정확도 | 약 95% (42봉지 중 40개 정확) |
| 분석 속도 (Colab T4) | 실시간 처리 가능 |

> 경계 이동 중인 봉지에서 2~3개 오탐/미탐 발생 가능.  
> 추가 정확도 향상은 경계 구간 학습 데이터 보강이 필요합니다.
