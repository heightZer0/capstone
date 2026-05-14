import cv2
import numpy as np
from PIL import Image, ImageDraw, ImageFont

FONT_PATH = '/home/jaehpd99/pill_project/fonts/NanumGothic.ttf'
_font_cache = {}


def _get_font(size: int = 22):
    if size not in _font_cache:
        try:
            _font_cache[size] = ImageFont.truetype(FONT_PATH, size)
        except Exception:
            _font_cache[size] = ImageFont.load_default()
    return _font_cache[size]


def _put_korean(img_bgr, text, pos, color=(255, 255, 255), font_size=22):
    img_rgb = cv2.cvtColor(img_bgr, cv2.COLOR_BGR2RGB)
    pil = Image.fromarray(img_rgb)
    draw = ImageDraw.Draw(pil)
    font = _get_font(font_size)
    # 그림자
    draw.text((pos[0]+1, pos[1]+1), text, font=font, fill=(0, 0, 0))
    draw.text(pos, text, font=font, fill=color)
    return cv2.cvtColor(np.array(pil), cv2.COLOR_RGB2BGR)


def draw_results(frame: np.ndarray, pouches: list[dict]) -> np.ndarray:
    """
    pouches: list of {pouch dict} + 'ocr_info' + 'detections' + 'summary'
    """
    out = frame.copy()

    for p in pouches:
        x0, x1 = p['x_start'], p['x_end']
        y0, y1 = p['y_start'], p['y_end']
        info    = p.get('ocr_info', {})
        summary = p.get('summary', {})
        dets    = p.get('detections', [])

        has_error = summary.get('error', 0) > 0
        box_color = (0, 0, 255) if has_error else (0, 255, 0)

        # 파우치 테두리
        cv2.rectangle(out, (x0, y0), (x1, y1), box_color, 2)

        # 알약 bbox (파우치 로컬 좌표 → 원본 프레임 좌표)
        for d in dets:
            bx1, by1, bx2, by2 = [int(v) for v in d['bbox']]
            color = (0, 0, 255) if d['label'] == 'Error_Pill' else (0, 200, 255)
            cv2.rectangle(out,
                          (x0 + bx1, y0 + by1),
                          (x0 + bx2, y0 + by2),
                          color, 2)
            cv2.putText(out, f"{d['label'].replace('_Pill','')} {d['score']:.2f}",
                        (x0 + bx1, y0 + by1 - 4),
                        cv2.FONT_HERSHEY_SIMPLEX, 0.4, color, 1)

        # 파우치 번호 + 상태 텍스트
        pouch_id = p.get('pouch_id', '?')
        label_text = f"#{pouch_id}  Pill:{summary.get('total', 0)}"
        label_color = (0, 80, 255) if has_error else (0, 255, 80)
        cv2.putText(out, label_text, (x0 + 4, y0 + 20),
                    cv2.FONT_HERSHEY_SIMPLEX, 0.6, label_color, 2)

    return out
