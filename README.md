# 알약 자동 검수 시스템

> 캡스톤 디자인 | 한성대학교

컨베이어 벨트 위를 지나가는 약봉지를 스마트폰으로 촬영하여 **알약 수량 오류를 자동으로 감지**하는 시스템입니다.  
AI 파이프라인(Python)과 Android 앱(Kotlin)으로 구성됩니다.

---

## 시스템 구성

```
약사 / 작업자
    │
    │  스마트폰으로 컨베이어 벨트 촬영 (1280×720, 가로 모드)
    ▼
[Android 앱]
    │
    │  영상 업로드  POST /analyze
    ▼
[AI 파이프라인 서버]  ← Google Colab GPU (T4)
    │  RTMDet 알약 탐지 + 봉지 추적 + 패턴 감지
    │
    │  JSON 결과 + 결과 영상 ID 반환
    ▼
[Android 앱]
    봉지별 알약 수 / 오류 봉지 표시 / 결과 영상 재생 / 검사 이력 관리
```

| 컴포넌트 | 기술 | 위치 |
|---------|------|------|
| AI 파이프라인 | Python, RTMDet, mmdetection | `inspection_pipeline/` |
| 서버 | FastAPI, uvicorn, pyngrok | `inspection_pipeline/server.py` |
| Colab 실행 | Jupyter Notebook | `colab_server.ipynb` |
| Android 앱 | Kotlin, Jetpack Compose, Room DB | `app/` |

---

## AI 파이프라인 상세

### 처리 흐름

```
입력 영상 (1280×720)
    │
    ▼
[1] ROI 추출  (y=100 ~ y=620)
    전체 프레임에서 컨베이어 벨트 띠 영역만 사용
    │
    ▼
[2] pouch_splitter.py — 봉지 경계 분리
    검은 텍스트 밀도 피크(find_peaks)로 봉지 중심 x좌표 탐지
    → 봉지별 crop / 텍스트 영역(좌 55%) / 알약 영역(우 45%) 반환
    │
    ▼
[3] pill_detector.py — RTMDet-tiny 알약 탐지
    mAP@50 = 0.909  |  score_thr = 0.35  |  NMS iou = 0.40
    탐지 결과를 x좌표 기준으로 각 봉지에 배분
    │
    ▼
[4] CountStabilizer — 카운트 안정화
    15프레임 슬라이딩 윈도우 최빈값으로 흔들림 제거
    │
    ▼
[5] PouchTracker — 봉지 ID 추적
    오른쪽 60% 이상에서 진입한 봉지에만 신규 ID 부여
    최대 30프레임 미탐지까지 동일 ID 유지
    │
    ▼
[6] 확정 로직 (CONFIRM_DELAY = 8)
    봉지가 화면에서 사라진 후 8 detection 프레임 뒤 최종 확정
    history 길이 < 13이면 유령 ID로 간주하고 무시
    │
    ▼
[7] PatternMonitor — 복약 패턴 감지
    확정된 봉지 수량을 누적, 길이 1~6 패턴 투표
    70% 이상 일치하는 패턴 확정 → 패턴과 다른 봉지 = 오류
    │
    ▼
[8] 최종 재계산
    영상 종료 후 최종 패턴 기준으로 전체 봉지 오류 재계산
    │
    ▼
[9] 결과 영상 생성 (ffmpeg H.264)
    전체 프레임에 봉지 bbox + 알약 bbox + Pill:N 레이블 합성
    -movflags +faststart 로 HTTP 스트리밍 최적화
```

### 주요 파라미터

| 파라미터 | 값 | 설명 |
|---------|-----|------|
| `ROI_Y_TOP / BOTTOM` | 100 / 620 | 유효 영역 (1280×720 기준) |
| `SCORE_THR` | 0.35 | 탐지 confidence 임계값 |
| `NMS iou_threshold` | 0.40 | 겹친 bbox 제거 |
| `CountStabilizer.window` | 15 | 카운트 안정화 프레임 수 |
| `PouchTracker.max_dist` | 250 | 봉지 추적 최대 이동 거리 (px) |
| `PouchTracker.max_missing` | 30 | ID 유지 최대 프레임 수 |
| `PouchTracker.entry_zone` | 0.6 | 신규 봉지 진입 기준 (오른쪽 60%) |
| `CONFIRM_DELAY` | 8 | 봉지 확정 대기 detection 프레임 수 |
| `TEXT_PROMINENCE` | 3000 | 봉지 경계 탐지 피크 최소 prominence |
| `MIN_POUCH_WIDTH` | 150 | 봉지 최소 너비 (px) |

### 모델 정보

| 항목 | 내용 |
|------|------|
| 아키텍처 | RTMDet-tiny |
| 학습 클래스 | 단일 클래스 (`Pill`) |
| 성능 | mAP@50 = **0.909** (epoch 273) |
| 입력 해상도 | 640×640 (내부 처리) |
| 가중치 | Google Drive 보관 (GitHub 미포함) |

---

## Android 앱 화면 구성

| 화면 | 설명 |
|------|------|
| SplashScreen | 앱 시작 로고 |
| OnboardingScreen | 사용 방법 안내 |
| HomeScreen | 서버 URL 설정, 최근 검사 이력 (클릭 시 상세 이동) |
| CameraScreen | 가로 모드 고정, ROI 가이드 오버레이, 자동 녹화 |
| LoadingScreen | 서버 분석 대기 |
| ResultScreen | 봉지별 알약 수 / 오류 표시 / 결과 영상 보기 |
| StatisticsScreen | 날짜별 검사 이력 캘린더 |
| InspectionDetailScreen | 전체 봉지 목록 (정상 ✓ / 오류 표시) |
| VideoPlayerScreen | ExoPlayer 결과 영상 재생 (가로 모드) |

### 로컬 DB 구조 (Room)

```
result              — 검사 1회 결과
  analysis_id (PK)
  result_code       — "NORMAL" / "ERROR"
  total_pouches     — 전체 봉지 수
  error_count       — 오류 봉지 수
  analyzed_time     — 검사 시각

error_object        — 오류 봉지 상세 (result 외래키)
  analysis_id
  pouch_no          — 오류 봉지 순번
  error_code        — "CNT" (수량 오류)
```

---

## API 명세

### `GET /health`
```json
{ "status": "ok" }
```

### `POST /analyze`

**Request** — `multipart/form-data`

| 필드 | 타입 | 설명 |
|------|------|------|
| `video` | File | 분석할 영상 (mp4) |

**Response**
```json
{
  "isError": true,
  "errorPouchNumbers": [3],
  "elapsedSeconds": 42,
  "pattern": [2, 2],
  "pouches": [
    { "pouchId": 1, "count": 2, "expected": 2, "error": false },
    { "pouchId": 2, "count": 2, "expected": 2, "error": false },
    { "pouchId": 3, "count": 1, "expected": 2, "error": true  }
  ],
  "videoId": "abc123"
}
```

### `GET /video/{video_id}`
결과 영상 파일 스트리밍 (mp4)

---

## 설치 및 실행

### Google Colab (권장)

1. `colab_server.ipynb` 열기
2. **런타임 → GPU (T4)** 선택
3. Google Drive에 아래 구조로 파일 업로드

```
MyDrive/pill_project/
  inspection_pipeline/   ← pipeline.py, server.py 등
  model/
    rtmdet_single_pill_aug_300.py
    best_coco_bbox_mAP_50_epoch_273.pth
  fonts/
    NanumGothic.ttf
```

4. 노트북 셀 순서대로 실행 → 마지막 셀에서 ngrok URL 확인
5. Android 앱 홈 화면 설정 아이콘에서 해당 URL 입력

### 로컬 실행 (NVIDIA GPU 필요)

```bash
conda create -n pill python=3.10
conda activate pill

pip install torch torchvision --index-url https://download.pytorch.org/whl/cu121
pip install mmengine
pip install mmcv -f https://download.openmmlab.com/mmcv/dist/cu121/torch2.3/index.html
pip install mmdet
pip install fastapi uvicorn python-multipart easyocr scipy pyngrok

export PILL_CONFIG=/path/to/rtmdet_single_pill_aug_300.py
export PILL_CHECKPOINT=/path/to/best_coco_bbox_mAP_50_epoch_273.pth

cd inspection_pipeline
uvicorn server:app --host 0.0.0.0 --port 8000
```

---

## 성능

| 항목 | 수치 |
|------|------|
| 알약 탐지 mAP@50 | 0.909 |
| 처리 속도 | Colab T4 기준 실시간 처리 가능 |
| 결과 영상 | H.264 인코딩, HTTP 스트리밍 최적화 |

---

## 디렉토리 구조

```
capstone/
├── app/                            # Android 앱
│   └── src/main/java/com/example/capstone/
│       ├── AppNavigation.kt
│       ├── InspectionResult.kt
│       ├── InspectionSharedViewModel.kt
│       ├── InspectionRepository.kt
│       ├── StatisticsViewModel.kt
│       ├── data/local/             # Room DB (entity, dao, database)
│       └── screen/                 # 각 화면 Composable
│
├── inspection_pipeline/
│   ├── pipeline.py                 # 메인 파이프라인
│   ├── pill_detector.py            # RTMDet 탐지 + CountStabilizer
│   ├── pouch_splitter.py           # 봉지 경계 분리
│   ├── ocr_reader.py               # EasyOCR (현재 비활성화)
│   ├── visualizer.py               # 결과 시각화
│   └── server.py                   # FastAPI 서버
│
├── model/
│   └── rtmdet_single_pill_aug_300.py
│
├── colab_server.ipynb
└── README.md
```
