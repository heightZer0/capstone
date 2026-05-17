import cv2
import numpy as np
from scipy.ndimage import uniform_filter1d
from scipy.signal import find_peaks

# 카메라 고정 ROI (1280x720 기준)
ROI_Y_TOP    = 100
ROI_Y_BOTTOM = 620

MIN_POUCH_WIDTH  = 150   # 탐지할 최소 파우치 너비 (px)
TEXT_AREA_RATIO  = 0.55  # 파우치 왼쪽 중 텍스트 영역 비율
SMOOTH_SIZE      = 30    # column smoothing 커널 크기
DARK_THR         = 100   # 검은 텍스트 threshold (0~255)
TEXT_PROMINENCE  = 3000  # 텍스트 밀도 피크 최소 prominence


def get_roi(frame: np.ndarray) -> np.ndarray:
    return frame[ROI_Y_TOP:ROI_Y_BOTTOM, :]


def _find_pouch_centers(roi: np.ndarray) -> list[int]:
    """검은 텍스트 밀도 피크로 각 파우치의 텍스트 라벨 중심 x좌표 반환"""
    gray = cv2.cvtColor(roi, cv2.COLOR_BGR2GRAY)
    _, dark = cv2.threshold(gray, DARK_THR, 255, cv2.THRESH_BINARY_INV)
    col_sum = uniform_filter1d(dark.sum(axis=0).astype(float), size=SMOOTH_SIZE)
    peaks, _ = find_peaks(col_sum, prominence=TEXT_PROMINENCE, distance=MIN_POUCH_WIDTH)
    return peaks.tolist()


def _centers_to_boundaries(centers: list[int], W: int) -> list[int]:
    """텍스트 라벨 중심들로부터 파우치 경계 x좌표 목록 반환"""
    if not centers:
        return [0, W]
    midpoints = [int((centers[i] + centers[i + 1]) / 2)
                 for i in range(len(centers) - 1)]
    return [0] + midpoints + [W]


def split_pouches(frame: np.ndarray) -> list[dict]:
    """
    프레임에서 파우치 목록 반환.
    각 dict:
        x_start, x_end, y_start, y_end   : 원본 프레임 좌표
        crop                              : BGR 전체 파우치 crop
        text_crop                         : 90도 회전된 텍스트 영역 (OCR용)
        pill_crop                         : 알약 영역 crop
    """
    roi = get_roi(frame)
    centers = _find_pouch_centers(roi)
    W = frame.shape[1]
    boundaries = _centers_to_boundaries(centers, W)

    pouches = []
    for i in range(len(boundaries) - 1):
        x0, x1 = boundaries[i], boundaries[i + 1]
        if x1 - x0 < MIN_POUCH_WIDTH:
            continue

        crop = frame[ROI_Y_TOP:ROI_Y_BOTTOM, x0:x1]
        split = int((x1 - x0) * TEXT_AREA_RATIO)
        raw_text = crop[:, :split]
        pill_crop = crop[:, split:]

        # 텍스트가 90도 회전 인쇄 → 시계 방향 회전해서 수평으로
        text_crop = cv2.rotate(raw_text, cv2.ROTATE_90_CLOCKWISE)

        pouches.append({
            'x_start':   x0,
            'x_end':     x1,
            'y_start':   ROI_Y_TOP,
            'y_end':     ROI_Y_BOTTOM,
            'crop':      crop,
            'text_crop': text_crop,
            'pill_crop': pill_crop,
        })

    return pouches
