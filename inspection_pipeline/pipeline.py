"""
메인 파이프라인.
사용법:
    python pipeline.py --input ../data/test.mp4 --output output.mp4
    python pipeline.py --input ../data/test.mp4 --output output.mp4 --skip 5
"""
import argparse
import base64
import cv2
import numpy as np
import subprocess
import time

from pouch_splitter import split_pouches, get_roi, ROI_Y_TOP, ROI_Y_BOTTOM
from ocr_reader import read_pouch_info
from pill_detector import detect_pills_in_roi, assign_detections_to_pouches, CountStabilizer
from visualizer import draw_results


class PouchTracker:
    """
    파우치 순번 ID를 프레임 진입 순서대로 부여.
    오른쪽에서 진입할 때만 새 ID 생성 → 잠깐 사라진 봉지가 재탐지돼도 번호 안 튀김.
    """

    def __init__(self, max_dist: int = 300, max_missing: int = 30,
                 entry_zone: float = 0.6):
        self.max_dist    = max_dist
        self.max_missing = max_missing
        self.entry_zone  = entry_zone   # 이 비율 이상 오른쪽에서 나타나야 신규 봉지
        self._next_id    = 1
        self._frame_w    = 1920
        self._tracked: dict[int, dict] = {}  # id -> {cx, missing}

    def update(self, pouches: list[dict], frame_w: int = None) -> None:
        if frame_w:
            self._frame_w = frame_w
        curr_centers = [(p['x_start'] + p['x_end']) / 2 for p in pouches]

        for info in self._tracked.values():
            info['missing'] += 1

        assigned: dict[int, int] = {}
        used: set[int] = set()

        for i, cc in enumerate(curr_centers):
            best_pid, best_dist = None, self.max_dist
            for pid, info in self._tracked.items():
                if pid in used:
                    continue
                d = abs(cc - info['cx'])
                if d < best_dist:
                    best_dist, best_pid = d, pid

            if best_pid is None:
                # 오른쪽 진입 구간에서 나타난 경우에만 신규 ID 부여
                if cc >= self._frame_w * self.entry_zone:
                    best_pid = self._next_id
                    self._next_id += 1
                    self._tracked[best_pid] = {'cx': cc, 'missing': 0}
                else:
                    # 중간/왼쪽 재탐지는 가장 가까운 기존 트랙에 붙임
                    if self._tracked:
                        best_pid = min(self._tracked,
                                       key=lambda pid: abs(self._tracked[pid]['cx'] - cc))
                        self._tracked[best_pid]['cx'] = cc
                        self._tracked[best_pid]['missing'] = 0
                    else:
                        best_pid = self._next_id
                        self._next_id += 1
                        self._tracked[best_pid] = {'cx': cc, 'missing': 0}
            else:
                self._tracked[best_pid]['cx'] = cc
                self._tracked[best_pid]['missing'] = 0

            if best_pid is not None:
                assigned[i] = best_pid
                used.add(best_pid)

        stale = [pid for pid, info in self._tracked.items()
                 if info['missing'] > self.max_missing]
        for pid in stale:
            del self._tracked[pid]

        for i, p in enumerate(pouches):
            p['pouch_id'] = assigned.get(i)


class PatternMonitor:
    """
    통과 완료된 봉지의 알약 수를 순서대로 기록하고 반복 패턴을 자동 감지.
    패턴과 다른 봉지는 오류로 표시.
    """

    def __init__(self, min_cycles: int = 2, max_pattern_len: int = 6):
        self.min_cycles      = min_cycles
        self.max_pattern_len = max_pattern_len
        self._records: list[dict] = []   # {pouch_id, count}
        self.pattern: list[int] = []     # 확정된 패턴
        self._recorded_ids: set = set()  # 중복 기록 방지

    def record(self, pouch_id: int, count: int, history: list = None) -> dict | None:
        """봉지 통과 완료 기록. 중복·0개는 무시. 패턴/오류 여부 반환."""
        if pouch_id is None or pouch_id in self._recorded_ids or count == 0:
            return None
        self._recorded_ids.add(pouch_id)
        self._records.append({'pouch_id': pouch_id, 'count': count, 'history': history or []})
        self._detect_pattern()
        expected = None
        error    = False
        if self.pattern:
            idx      = (len(self._records) - 1) % len(self.pattern)
            expected = self.pattern[idx]
            error    = (count != expected)
        return {'pouch_id': pouch_id, 'count': count,
                'expected': expected, 'error': error,
                'pattern': self.pattern[:]}

    def _detect_pattern(self):
        sorted_records = sorted(self._records, key=lambda r: r['pouch_id'])
        counts = [r['count'] for r in sorted_records]
        n = len(counts)
        best_pattern, best_score = [], 0.0
        for length in range(1, self.max_pattern_len + 1):
            if n < length * self.min_cycles:
                continue
            # 각 위치마다 최빈값으로 패턴 추론
            pattern = []
            for pos in range(length):
                vals = [counts[i] for i in range(n) if i % length == pos]
                pattern.append(max(set(vals), key=vals.count))
            score = sum(1 for i in range(n) if counts[i] == pattern[i % length]) / n
            if score > best_score:
                best_score, best_pattern = score, pattern
        self.pattern = best_pattern if best_score >= 0.7 else []

    @property
    def records(self):
        return list(self._records)


def _interp_pouches(prev: list, curr: list, alpha: float) -> list:
    """prev→curr 사이를 alpha(0=prev, 1=curr)로 선형 보간."""
    if not prev or not curr or len(prev) != len(curr):
        return curr or prev
    result = []
    for pp, cp in zip(prev, curr):
        p = dict(cp)
        pd = sorted(pp.get('detections', []), key=lambda d: (d['bbox'][0] + d['bbox'][2]) / 2)
        cd = sorted(cp.get('detections', []), key=lambda d: (d['bbox'][0] + d['bbox'][2]) / 2)
        if len(pd) == len(cd) and pd:
            interp = []
            for a, b in zip(pd, cd):
                bbox = [a['bbox'][i] * (1 - alpha) + b['bbox'][i] * alpha for i in range(4)]
                interp.append({**b, 'bbox': bbox})
            p['detections'] = interp
        else:
            p['detections'] = cd
        result.append(p)
    return result


def _run_detection(frame: np.ndarray, pouches: list) -> None:
    # 풀 프레임 ROI로 한 번에 탐지 → 학습 스케일(1920x1080)과 일치
    roi = get_roi(frame)
    all_dets = detect_pills_in_roi(roi)
    assign_detections_to_pouches(all_dets, pouches)


def _run_ocr(pouches: list) -> None:
    for p in pouches:
        p['ocr_info'] = read_pouch_info(p['text_crop'])


def process_frame(frame, run_det: bool = True, run_ocr: bool = True,
                  last_pouches: list = None):
    pouches = split_pouches(frame)

    # 이전 OCR 결과 이어받기 (파우치 수가 같을 때)
    if last_pouches and len(pouches) == len(last_pouches):
        for p, lp in zip(pouches, last_pouches):
            p['ocr_info']   = lp.get('ocr_info', {})
            p['detections'] = lp.get('detections', [])
            p['summary']    = lp.get('summary', {'total': 0, 'normal': 0, 'error': 0})

    if run_det:
        _run_detection(frame, pouches)
    if run_ocr:
        _run_ocr(pouches)

    return pouches


def run_video(input_path: str, output_path: str,
              det_skip: int = 1, ocr_skip: int = 30, show: bool = False):
    cap = cv2.VideoCapture(input_path)
    fps    = cap.get(cv2.CAP_PROP_FPS)
    width  = int(cap.get(cv2.CAP_PROP_FRAME_WIDTH))
    height = int(cap.get(cv2.CAP_PROP_FRAME_HEIGHT))
    total  = int(cap.get(cv2.CAP_PROP_FRAME_COUNT))

    out = cv2.VideoWriter(output_path,
                          cv2.VideoWriter_fourcc(*'mp4v'),
                          fps, (width, height))

    frame_idx        = 0
    prev_det_pouches = []
    curr_det_pouches = []
    last_pouches     = []
    frames_since_det = 0
    stabilizer          = CountStabilizer(window=15)
    tracker             = PouchTracker(max_dist=250, max_missing=30)
    pattern_monitor     = PatternMonitor()
    count_history: dict = {}   # pouch_id -> 카운트 누적 리스트
    pending_confirm: dict = {}  # pouch_id -> 사라진 후 경과 detection 횟수
    prev_pouch_ids: set = set()
    CONFIRM_DELAY = 8           # N번 연속 미탐지 시에만 확정
    t_start             = time.time()

    print(f"입력: {input_path}  ({width}x{height}, {fps:.0f}fps, {total}프레임)")
    print(f"탐지: {det_skip}프레임마다  OCR: {ocr_skip}프레임마다  출력: {fps:.0f}fps")

    while True:
        ret, frame = cap.read()
        if not ret:
            break

        run_det = (frame_idx % det_skip == 0)
        run_ocr = False

        if run_det or run_ocr:
            last_pouches = process_frame(frame,
                                         run_det=run_det,
                                         run_ocr=run_ocr,
                                         last_pouches=last_pouches)
        if run_det:
            tracker.update(last_pouches, frame_w=width)

            curr_pouch_ids = {p['pouch_id'] for p in last_pouches if p['pouch_id']}

            stabilizer.update(last_pouches)

            # stabilizer 후 안정화된 카운트 기록
            for p in last_pouches:
                pid = p['pouch_id']
                if pid is None:
                    continue
                count = p['summary']['total']
                if count > 0:
                    count_history.setdefault(pid, []).append(count)

            # 이번 프레임에 사라진 봉지 → pending에 추가
            for pid in prev_pouch_ids - curr_pouch_ids:
                pending_confirm.setdefault(pid, 0)
            # 다시 나타난 봉지 → pending 취소
            for pid in curr_pouch_ids & set(pending_confirm):
                del pending_confirm[pid]
            # pending 카운트 증가 → CONFIRM_DELAY 이상이면 확정
            to_confirm = []
            for pid in list(pending_confirm):
                pending_confirm[pid] += 1
                if pending_confirm[pid] >= CONFIRM_DELAY:
                    to_confirm.append(pid)
                    del pending_confirm[pid]
            for pid in to_confirm:
                history = count_history.pop(pid, [])
                if len(history) < 13:
                    continue
                final_count = max(set(history), key=history.count)
                result = pattern_monitor.record(pid, final_count, history=history)
                if result is not None:
                    status = f"[ERROR] expected {result['expected']}" if result['error'] else "OK"
                    print(f"  >> #{pid} 확정: 알약 {result['count']}개  {status}"
                          f"  패턴={result['pattern']}")

            prev_pouch_ids = curr_pouch_ids
            prev_det_pouches = curr_det_pouches
            curr_det_pouches = last_pouches
            frames_since_det = 0
        else:
            frames_since_det += 1

        # det_skip=1이면 보간 불필요, 아니면 이전→현재 선형 보간
        if det_skip <= 1 or not prev_det_pouches:
            display_pouches = curr_det_pouches
        else:
            alpha = min(1.0, frames_since_det / det_skip)
            display_pouches = _interp_pouches(prev_det_pouches, curr_det_pouches, alpha)
        annotated = draw_results(frame, display_pouches)
        out.write(annotated)

        if run_det and last_pouches and frame_idx % 30 == 0:
            elapsed = time.time() - t_start
            print(f"  [{frame_idx:4d}/{total}] {elapsed:.1f}s | 파우치 {len(last_pouches)}개", end='')
            for p in last_pouches:
                s = p.get('summary', {})
                pid = p.get('pouch_id', '?')
                print(f"  #{pid} {'불량' if s.get('error', 0) > 0 else 'OK'}({s.get('total', 0)}알)", end='')
            print()

        if show:
            cv2.imshow('Inspection', annotated)
            if cv2.waitKey(1) & 0xFF == ord('q'):
                break

        frame_idx += 1

    cap.release()
    out.release()
    if show:
        cv2.destroyAllWindows()
    print(f"\n완료. 결과 저장: {output_path}")

    # 최종 요약
    records = pattern_monitor.records
    if records:
        final_pattern = pattern_monitor.pattern
        sorted_records = sorted(records, key=lambda r: r['pouch_id'])

        # 최종 패턴 기준으로 오류 재계산
        if final_pattern:
            for i, r in enumerate(sorted_records):
                expected = final_pattern[i % len(final_pattern)]
                r['expected'] = expected
                r['error']    = (r['count'] != expected)

        print("\n===== 봉지별 알약 수 요약 =====")
        for r in sorted_records:
            print(f"  봉지 #{r['pouch_id']:>3d}: 알약 {r['count']}개  기록={r.get('history', [])}")

        if final_pattern:
            print(f"\n  감지된 패턴: {final_pattern}")
        else:
            print(f"\n  패턴 미감지 (봉지 수 부족 또는 불규칙)")

        errors = [r for r in sorted_records if r.get('error')]
        if errors:
            print(f"\n  ⚠ 오류 봉지 ({len(errors)}개):")
            for e in errors:
                idx = sorted_records.index(e) + 1
                print(f"    {idx}번째 봉지 (ID #{e['pouch_id']}): {e['count']}개 (예상: {e['expected']}개)")
        else:
            print("  오류 없음")


def analyze_video(input_path: str, output_path: str = None,
                  det_skip: int = 2, ocr_skip: int = 30) -> dict:
    """비디오 분석 후 결과 dict 반환 (API 서버용)"""
    cap    = cv2.VideoCapture(input_path)
    width  = int(cap.get(cv2.CAP_PROP_FRAME_WIDTH))
    height = int(cap.get(cv2.CAP_PROP_FRAME_HEIGHT))
    fps    = cap.get(cv2.CAP_PROP_FPS) or 30.0

    ffmpeg_proc = None
    if output_path:
        ffmpeg_proc = subprocess.Popen(
            ['ffmpeg', '-y',
             '-f', 'rawvideo', '-vcodec', 'rawvideo',
             '-s', f'{width}x{height}',
             '-pix_fmt', 'bgr24', '-r', str(fps),
             '-i', 'pipe:0',
             '-vcodec', 'libx264', '-preset', 'fast',
             '-pix_fmt', 'yuv420p',
             '-g', '30',
             '-movflags', '+faststart',
             output_path],
            stdin=subprocess.PIPE,
            stdout=subprocess.DEVNULL,
            stderr=subprocess.DEVNULL
        )

    frame_idx         = 0
    last_pouches      = []
    stabilizer        = CountStabilizer(window=15)
    tracker           = PouchTracker(max_dist=250, max_missing=30)
    pattern_monitor   = PatternMonitor()
    count_history: dict   = {}
    pending_confirm: dict = {}
    prev_pouch_ids: set   = set()
    pouch_best_frame: dict = {}   # pid -> (frame, pouch_dict) 오류 봉지 사진용
    CONFIRM_DELAY         = 8
    t_start               = time.time()

    while True:
        ret, frame = cap.read()
        if not ret:
            break
        run_det = (frame_idx % det_skip == 0)
        run_ocr = False
        if run_det or run_ocr:
            last_pouches = process_frame(frame, run_det=run_det, run_ocr=run_ocr,
                                         last_pouches=last_pouches)
        if run_det:
            tracker.update(last_pouches, frame_w=width)
            curr_pouch_ids = {p['pouch_id'] for p in last_pouches if p['pouch_id']}
            stabilizer.update(last_pouches)
            for p in last_pouches:
                pid = p['pouch_id']
                if pid is None:
                    continue
                count = p['summary']['total']
                if count > 0:
                    count_history.setdefault(pid, []).append(count)
                    pouch_best_frame[pid] = (frame.copy(), dict(p))
            for pid in prev_pouch_ids - curr_pouch_ids:
                pending_confirm.setdefault(pid, 0)
            for pid in curr_pouch_ids & set(pending_confirm):
                del pending_confirm[pid]
            to_confirm = []
            for pid in list(pending_confirm):
                pending_confirm[pid] += 1
                if pending_confirm[pid] >= CONFIRM_DELAY:
                    to_confirm.append(pid)
                    del pending_confirm[pid]
            for pid in to_confirm:
                history = count_history.pop(pid, [])
                if len(history) < 13:
                    continue
                final_count = max(set(history), key=history.count)
                pattern_monitor.record(pid, final_count, history=history)
            prev_pouch_ids = curr_pouch_ids

        if ffmpeg_proc is not None:
            annotated = draw_results(frame, last_pouches)
            ffmpeg_proc.stdin.write(annotated.tobytes())

        frame_idx += 1

    cap.release()
    if ffmpeg_proc is not None:
        ffmpeg_proc.stdin.close()
        ffmpeg_proc.wait()

    elapsed        = int(time.time() - t_start)
    records        = pattern_monitor.records
    final_pattern  = pattern_monitor.pattern
    sorted_records = sorted(records, key=lambda r: r['pouch_id'])
    if final_pattern:
        for i, r in enumerate(sorted_records):
            exp = final_pattern[i % len(final_pattern)]
            r['expected'] = exp
            r['error']    = (r['count'] != exp)
    errors = [r for r in sorted_records if r.get('error')]

    error_crops: dict = {}
    for r in errors:
        pid = r['pouch_id']
        if pid in pouch_best_frame:
            f, p = pouch_best_frame[pid]
            crop = f[p['y_start']:p['y_end'], p['x_start']:p['x_end']]
            for d in p.get('detections', []):
                bx1, by1, bx2, by2 = [int(v) for v in d['bbox']]
                cv2.rectangle(crop, (bx1, by1), (bx2, by2), (0, 200, 255), 2)
            _, buf = cv2.imencode('.jpg', crop, [cv2.IMWRITE_JPEG_QUALITY, 85])
            error_crops[str(pid)] = base64.b64encode(buf).decode()

    # 대표 썸네일 — 정상 봉지 중 첫 번째, 없으면 오류 봉지 중 첫 번째
    error_pids = {r['pouch_id'] for r in errors}
    normal_records = [r for r in sorted_records if r['pouch_id'] not in error_pids]
    thumbnail_candidates = normal_records if normal_records else sorted_records
    thumbnail_crop = None
    for r in thumbnail_candidates:
        pid = r['pouch_id']
        if pid in pouch_best_frame:
            f, p = pouch_best_frame[pid]
            crop = f[p['y_start']:p['y_end'], p['x_start']:p['x_end']]
            _, buf = cv2.imencode('.jpg', crop, [cv2.IMWRITE_JPEG_QUALITY, 80])
            thumbnail_crop = base64.b64encode(buf).decode()
            break

    return {
        'isError':           len(errors) > 0,
        'errorPouchNumbers': [r['pouch_id'] for r in errors],
        'elapsedSeconds':    elapsed,
        'pattern':           final_pattern,
        'errorCrops':        error_crops,
        'thumbnailCrop':     thumbnail_crop,
        'pouches': [
            {'pouchId':  r['pouch_id'],
             'count':    r['count'],
             'expected': r.get('expected'),
             'error':    r.get('error', False)}
            for r in sorted_records
        ],
    }


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--input',  required=True,  help='입력 영상 경로')
    parser.add_argument('--output', required=True,  help='출력 영상 경로')
    parser.add_argument('--det_skip', type=int, default=2,  help='탐지 N프레임마다 1회 (기본 2)')
    parser.add_argument('--ocr_skip', type=int, default=30, help='OCR N프레임마다 1회 (기본 30)')
    parser.add_argument('--show',     action='store_true',  help='실시간 화면 출력')
    args = parser.parse_args()

    run_video(args.input, args.output,
              det_skip=args.det_skip, ocr_skip=args.ocr_skip, show=args.show)
