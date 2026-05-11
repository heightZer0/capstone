import os
import numpy as np
from collections import deque
from mmdet.apis import init_detector, inference_detector

CONFIG_PATH     = os.environ.get('PILL_CONFIG',     '/home/jaehpd99/mmdetection/work_dirs/rtmdet_single_pill_aug_300/rtmdet_single_pill_aug_300.py')
CHECKPOINT_PATH = os.environ.get('PILL_CHECKPOINT', '/home/jaehpd99/mmdetection/work_dirs/rtmdet_single_pill_aug_300/best_coco_bbox_mAP_50_epoch_273.pth')
SCORE_THR       = 0.35   # 빛 반사 오탐 방지 (학습 score_thr=0.05, 추론은 별도 적용)
DEVICE          = 'cuda:0'

_model = None


def _get_model():
    global _model
    if _model is None:
        _model = init_detector(CONFIG_PATH, CHECKPOINT_PATH, device=DEVICE)
        _model.test_cfg.nms['iou_threshold'] = 0.40
        _model.bbox_head.test_cfg.nms['iou_threshold'] = 0.40
    return _model


def detect_pills_in_roi(roi: np.ndarray) -> list[dict]:
    """
    ROI 전체 이미지에서 알약 탐지.
    Returns list of:
        {
            'label': str,       # 'Pill' | 'Error_Pill'
            'score': float,
            'bbox':  [x1, y1, x2, y2],  # ROI 내부 좌표
        }
    """
    model = _get_model()
    result = inference_detector(model, roi)

    classes = model.dataset_meta.get('classes', ())
    detections = []

    pred = result.pred_instances
    for bbox, label, score in zip(pred.bboxes, pred.labels, pred.scores):
        if float(score) < SCORE_THR:
            continue
        detections.append({
            'label': classes[int(label)] if int(label) < len(classes) else str(label),
            'score': float(score),
            'bbox':  [float(v) for v in bbox],
        })

    return detections


def assign_detections_to_pouches(detections: list[dict], pouches: list[dict]) -> None:
    """
    ROI 좌표계의 탐지 결과를 x좌표 기준으로 각 파우치에 배분.
    각 pouch dict에 'detections'와 'summary' 키를 in-place로 추가.
    """
    for p in pouches:
        p['detections'] = []

    for det in detections:
        cx = (det['bbox'][0] + det['bbox'][2]) / 2
        for p in pouches:
            if p['x_start'] <= cx < p['x_end']:
                # bbox를 파우치 로컬 좌표로 변환
                local_det = dict(det)
                b = det['bbox']
                local_det['bbox'] = [b[0] - p['x_start'], b[1],
                                     b[2] - p['x_start'], b[3]]
                p['detections'].append(local_det)
                break

    for p in pouches:
        p['summary'] = count_summary(p['detections'])


def count_summary(detections: list[dict]) -> dict:
    total = len(detections)
    return {
        'total':  total,
        'normal': total,
        'error':  0,
    }



class CountStabilizer:
    """
    파우치별 최근 N프레임 카운트 버퍼를 유지하고 최빈값을 안정 카운트로 반환.
    파우치 수가 바뀌면(파우치 진입/이탈) 버퍼를 리셋한다.
    """

    def __init__(self, window: int = 15):
        self.window = window
        self._buffers: dict[int, deque] = {}
        self._n_pouches = -1

    def update(self, pouches: list[dict]) -> None:
        for p in pouches:
            pid = p.get('pouch_id')
            if pid is None:
                continue
            raw = p['summary']['total']
            if pid not in self._buffers:
                self._buffers[pid] = deque(maxlen=self.window)
            self._buffers[pid].append(raw)
            buf = list(self._buffers[pid])
            stable = max(set(buf), key=buf.count)
            p['summary']['total']  = stable
            p['summary']['normal'] = stable
