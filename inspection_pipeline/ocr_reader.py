import re
import numpy as np
import easyocr

_reader = None

def _get_reader():
    global _reader
    if _reader is None:
        _reader = easyocr.Reader(['ko', 'en'], gpu=True, verbose=False)
    return _reader


TIME_KEYWORDS = ('아침', '점심', '저녁', '취침')


def read_pouch_info(text_crop: np.ndarray) -> dict:
    """
    90도 회전된 텍스트 crop에서 OCR 실행.
    Returns:
        number  : int or None    파우치 번호 (1, 2, 3 ...)
        time    : str or None    '아침' | '점심' | '저녁' | '취침'
        date    : str or None    'YYYY-MM-DD'
        raw     : list[str]      인식된 전체 텍스트 목록
    """
    reader = _get_reader()
    result = reader.readtext(text_crop)

    number, time_label, date, texts = None, None, None, []

    for (_, text, conf) in result:
        if conf < 0.25:
            continue
        t = text.strip()
        texts.append(t)

        # 파우치 번호: 1~99 단독 숫자
        if re.fullmatch(r'\d{1,2}', t) and number is None:
            number = int(t)

        # 복용 시간
        for kw in TIME_KEYWORDS:
            if kw in t and time_label is None:
                time_label = kw

        # 날짜
        m = re.search(r'\d{4}[-./]\d{2}[-./]\d{2}', t)
        if m and date is None:
            date = m.group().replace('.', '-').replace('/', '-')

    return {
        'number': number,
        'time':   time_label,
        'date':   date,
        'raw':    texts,
    }
