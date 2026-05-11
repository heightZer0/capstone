"""
알약 검수 파이프라인 API 서버
실행: uvicorn server:app --host 0.0.0.0 --port 8000
"""
import os
import tempfile
from fastapi import FastAPI, File, UploadFile, HTTPException
from fastapi.responses import JSONResponse

from pipeline import analyze_video

app = FastAPI(title="알약 검수 API")


@app.get("/health")
def health():
    return {"status": "ok"}


@app.post("/analyze")
async def analyze(video: UploadFile = File(...)):
    suffix   = os.path.splitext(video.filename or 'video')[1] or '.mp4'
    tmp_path = None
    try:
        with tempfile.NamedTemporaryFile(suffix=suffix, delete=False) as tmp:
            tmp.write(await video.read())
            tmp_path = tmp.name
        result = analyze_video(tmp_path)
        return JSONResponse(content=result)
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
    finally:
        if tmp_path and os.path.exists(tmp_path):
            os.unlink(tmp_path)
