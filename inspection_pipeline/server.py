"""
알약 검수 파이프라인 API 서버
실행: uvicorn server:app --host 0.0.0.0 --port 8000
"""
import os
import tempfile
import uuid
from fastapi import FastAPI, File, UploadFile, HTTPException
from fastapi.responses import FileResponse, JSONResponse

from pipeline import analyze_video

app = FastAPI(title="알약 검수 API")

VIDEO_DIR = tempfile.mkdtemp()


@app.get("/health")
def health():
    return {"status": "ok"}


@app.post("/analyze")
async def analyze(video: UploadFile = File(...)):
    suffix   = os.path.splitext(video.filename or 'video')[1] or '.mp4'
    tmp_path = None
    video_id = uuid.uuid4().hex
    output_path = os.path.join(VIDEO_DIR, f"{video_id}.mp4")
    try:
        with tempfile.NamedTemporaryFile(suffix=suffix, delete=False) as tmp:
            tmp.write(await video.read())
            tmp_path = tmp.name
        result = analyze_video(tmp_path, output_path=output_path)
        result['videoId'] = video_id
        return JSONResponse(content=result)
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
    finally:
        if tmp_path and os.path.exists(tmp_path):
            os.unlink(tmp_path)


@app.get("/video/{video_id}")
def get_video(video_id: str):
    if not video_id.isalnum():
        raise HTTPException(status_code=400, detail="Invalid video ID")
    path = os.path.join(VIDEO_DIR, f"{video_id}.mp4")
    if not os.path.exists(path):
        raise HTTPException(status_code=404, detail="Video not found")
    return FileResponse(path, media_type="video/mp4")
