"""
Edge Smartboard AI — Bridge Backend Server
==========================================
Standalone FastAPI server that matches exactly what the Android app expects.
Run with: python edge_server.py

Default credentials:
  email:    admin@edge.school
  password: Admin@1234
"""

from fastapi import FastAPI, HTTPException, Depends, Header
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from typing import List, Optional
import uvicorn, datetime, secrets, hashlib, json, os

app = FastAPI(title="Edge Smartboard AI Backend", version="1.0.0")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

# ── Simple in-memory stores ──────────────────────────────────────
TOKENS: dict[str, dict] = {}   # token -> user info

# Default user (hashed with sha256 for simplicity)
def hash_pw(pw: str) -> str:
    return hashlib.sha256(pw.encode()).hexdigest()

USERS = {
    "admin@edge.school": {
        "id": "usr-001",
        "name": "Admin User",
        "email": "admin@edge.school",
        "school": "Edge Academy",
        "role": "admin",
        "hashed_password": hash_pw("admin123"),
    },
    "admin@school.edu": {
        "id": "usr-003",
        "name": "Admin User",
        "email": "admin@school.edu",
        "school": "Edge Academy",
        "role": "admin",
        "hashed_password": hash_pw("admin123"),
    },
    "teacher@edge.school": {
        "id": "usr-002",
        "name": "Dr. Smith",
        "email": "teacher@edge.school",
        "school": "Edge Academy",
        "role": "teacher",
        "hashed_password": hash_pw("teacher123"),
    },
}

SESSIONS_DB: list[dict] = []
REPORTS_DB:  list[dict] = []

def get_current_user(authorization: str = Header(...)):
    token = authorization.replace("Bearer ", "").strip()
    if token not in TOKENS:
        raise HTTPException(status_code=401, detail="Invalid or expired token")
    return TOKENS[token]

# ── Models ────────────────────────────────────────────────────────
class LoginRequest(BaseModel):
    email: str
    password: str

# ── Auth ─────────────────────────────────────────────────────────
@app.post("/login")
def login(body: LoginRequest):
    user = USERS.get(body.email)
    if not user or user["hashed_password"] != hash_pw(body.password):
        raise HTTPException(status_code=401, detail="Invalid email or password")

    token = secrets.token_hex(32)
    TOKENS[token] = user

    return {
        "access_token": token,
        "token_type": "bearer",
        "user": {
            "id":     user["id"],
            "name":   user["name"],
            "email":  user["email"],
            "school": user["school"],
            "role":   user["role"],
        }
    }

# ── Status ───────────────────────────────────────────────────────
@app.get("/status")
def get_status(user=Depends(get_current_user)):
    import random
    return {
        "cpuPercent":  round(random.uniform(20, 60), 1),
        "gpuPercent":  round(random.uniform(10, 70), 1),
        "gpuMemoryMb": random.randint(1024, 6144),
        "queues": [
            {"name": "CPU Queue",     "length": random.randint(0, 8),  "status": "PROCESSING", "latencyMs": 12},
            {"name": "GPU Queue",     "length": random.randint(0, 5),  "status": "IDLE",       "latencyMs": 45},
            {"name": "Vision Queue",  "length": random.randint(0, 12), "status": "PROCESSING", "latencyMs": 78},
            {"name": "Audio Queue",   "length": random.randint(0, 6),  "status": "COMPLETED",  "latencyMs": 23},
        ]
    }

# ── Dashboard ────────────────────────────────────────────────────
@app.get("/dashboard")
def get_dashboard(user=Depends(get_current_user)):
    import random
    return {
        "totalSessions":    len(SESSIONS_DB) + 247,
        "todaySessions":    12,
        "avgTeachingScore": 84.3,
        "gpuUtilization":   round(random.uniform(30, 80), 1),
        "cpuUtilization":   round(random.uniform(20, 60), 1),
        "storageUsedGb":    128.4,
        "uploadSuccessRate": 97.2,
        "weeklyData":       [72.0, 85.0, 78.0, 91.0, 88.0, 76.0, 84.0],
        "monthlyData":      [80.0, 83.0, 81.0, 87.0],
        "departmentData":   {"Science": 88.0, "Math": 82.0, "English": 85.0, "History": 79.0}
    }

# ── Sessions ─────────────────────────────────────────────────────
@app.get("/session")
def list_sessions(user=Depends(get_current_user)):
    return SESSIONS_DB

@app.get("/session/{session_id}")
def get_session(session_id: str, user=Depends(get_current_user)):
    for s in SESSIONS_DB:
        if s["sessionId"] == session_id:
            return s
    raise HTTPException(status_code=404, detail="Session not found")

# ── Upload ───────────────────────────────────────────────────────
from fastapi import UploadFile, File as FastAPIFile
import shutil

UPLOAD_DIR = os.path.join(os.path.dirname(__file__), "uploads")
os.makedirs(UPLOAD_DIR, exist_ok=True)

@app.post("/capture/upload")
async def upload_session(
    file: UploadFile = FastAPIFile(None),
    user=Depends(get_current_user)
):
    session_id = f"SID-{secrets.token_hex(4).upper()}"

    saved_filename = None
    if file and file.filename:
        ext = os.path.splitext(file.filename)[-1] or ".mp4"
        save_path = os.path.join(UPLOAD_DIR, f"{session_id}{ext}")
        with open(save_path, "wb") as f:
            shutil.copyfileobj(file.file, f)
        saved_filename = save_path
        print(f"[UPLOAD] Saved {file.filename} → {save_path} ({os.path.getsize(save_path)} bytes)")

    SESSIONS_DB.append({
        "sessionId":       session_id,
        "teacherName":     user["name"],
        "subject":         "General",
        "classRoom":       "Room 101",
        "department":      "Science",
        "date":            int(datetime.datetime.utcnow().timestamp() * 1000),
        "durationSeconds": 3600,
        "imageCount":      120,
        "status":          "PROCESSING",
        "score":           None,
        "filePath":        saved_filename,
    })
    return {"success": True, "sessionId": session_id, "message": "Upload received. Processing started."}

# ── Teacher Analysis Upload ───────────────────────────────────────
from fastapi import Form
import random as _random

TEACHER_UPLOAD_DIR = os.path.join(os.path.dirname(__file__), "uploads", "teachers")
os.makedirs(TEACHER_UPLOAD_DIR, exist_ok=True)

@app.post("/teacher/upload")
async def upload_teacher_session(
    video:          UploadFile = FastAPIFile(...),
    frames:         List[UploadFile] = FastAPIFile(default=[]),
    audio:          Optional[UploadFile] = FastAPIFile(default=None),
    teacher_id:     str = Form(...),
    teacher_name:   str = Form(...),
    subject:        str = Form(...),
    duration_ms:    str = Form("0"),
    frame_count:    str = Form("0"),
    file_size_bytes:str = Form("0"),
    user=Depends(get_current_user)
):
    import uuid
    session_id = f"TS-{uuid.uuid4().hex[:8].upper()}"

    # Create teacher-specific output dir
    teacher_dir = os.path.join(TEACHER_UPLOAD_DIR, teacher_id, session_id)
    os.makedirs(teacher_dir, exist_ok=True)

    # 1. Save video
    video_path = os.path.join(teacher_dir, video.filename or "video.mp4")
    with open(video_path, "wb") as f:
        shutil.copyfileobj(video.file, f)
    video_size = os.path.getsize(video_path)
    print(f"[TEACHER UPLOAD] Video → {video_path} ({video_size:,} bytes)")

    # 2. Save frames
    frames_dir = os.path.join(teacher_dir, "frames")
    os.makedirs(frames_dir, exist_ok=True)
    saved_frames = []
    for i, frame in enumerate(frames):
        frame_path = os.path.join(frames_dir, frame.filename or f"frame_{i:03d}.jpg")
        with open(frame_path, "wb") as f:
            shutil.copyfileobj(frame.file, f)
        saved_frames.append(frame_path)
    print(f"[TEACHER UPLOAD] {len(saved_frames)} frames → {frames_dir}")

    # 3. Save audio
    audio_saved = False
    if audio and audio.filename:
        audio_path = os.path.join(teacher_dir, audio.filename)
        with open(audio_path, "wb") as f:
            shutil.copyfileobj(audio.file, f)
        audio_saved = True
        print(f"[TEACHER UPLOAD] Audio → {audio_path}")

    # 4. Write metadata JSON
    meta = {
        "sessionId":      session_id,
        "teacherId":      teacher_id,
        "teacherName":    teacher_name,
        "subject":        subject,
        "uploadedBy":     user["name"],
        "uploadedAt":     datetime.datetime.utcnow().isoformat(),
        "durationMs":     int(duration_ms),
        "framesExtracted":len(saved_frames),
        "audioIncluded":  audio_saved,
        "fileSizeBytes":  int(file_size_bytes),
        "videoPath":      video_path,
        "framesDir":      frames_dir,
        "status":         "QUEUED_FOR_ANALYSIS"
    }
    with open(os.path.join(teacher_dir, "metadata.json"), "w") as mf:
        json.dump(meta, mf, indent=2)

    # 5. Add to sessions DB
    score = round(_random.uniform(72, 96), 1)
    SESSIONS_DB.append({
        "sessionId":       session_id,
        "teacherName":     teacher_name,
        "teacherId":       teacher_id,
        "subject":         subject,
        "date":            int(datetime.datetime.utcnow().timestamp() * 1000),
        "durationSeconds": int(duration_ms) // 1000,
        "imageCount":      len(saved_frames),
        "status":          "PROCESSING",
        "score":           score,
        "filePath":        video_path,
    })

    print(f"[TEACHER UPLOAD] Session {session_id} stored ✓")
    return {
        "success":        True,
        "sessionId":      session_id,
        "teacherId":      teacher_id,
        "framesReceived": len(saved_frames),
        "audioReceived":  audio_saved,
        "message":        f"Received {len(saved_frames)} frames {'+ audio' if audio_saved else '(no audio)'}. AI analysis queued."
    }

# ── Reports ──────────────────────────────────────────────────────
@app.get("/reports")
def list_reports(user=Depends(get_current_user)):
    return REPORTS_DB

@app.get("/reports/{session_id}")
def get_report(session_id: str, user=Depends(get_current_user)):
    for r in REPORTS_DB:
        if r["sessionId"] == session_id:
            return r
    # Return a demo report
    return {
        "sessionId":          session_id,
        "overallScore":       84.3,
        "lessonCoverage":     88.0,
        "studentEngagement":  79.0,
        "speakingTime":       72.0,
        "boardUsage":         85.0,
        "visualTeachingScore":82.0,
        "voiceClarity":       91.0,
        "curriculumAlignment":86.0,
        "strengths":      ["Clear explanations", "Good board usage", "Engaging voice"],
        "weaknesses":     ["Student interaction could improve", "Lesson pacing"],
        "recommendations":["Add more Q&A segments", "Use visual aids more frequently"]
    }

# ── History ──────────────────────────────────────────────────────
@app.get("/history")
def get_history(page: int = 0, limit: int = 20, user=Depends(get_current_user)):
    demo_history = [
        {
            "session": {
                "sessionId": f"SID-DEMO-{i:03d}",
                "teacherName": user["name"],
                "subject": ["Math", "Science", "English", "History"][i % 4],
                "classRoom": f"Room {100 + i}",
                "department": "Science",
                "date": int((datetime.datetime.utcnow() - datetime.timedelta(days=i)).timestamp() * 1000),
                "durationSeconds": 3600 + i * 60,
                "imageCount": 100 + i * 10,
                "status": "UPLOADED",
                "score": round(75 + (i % 20), 1)
            },
            "report": None
        }
        for i in range(page * limit, (page + 1) * limit)
    ]
    return demo_history

# ── Live Frame ───────────────────────────────────────────────────
@app.get("/live/{session_id}")
def get_live_frame(session_id: str, user=Depends(get_current_user)):
    import random
    return {
        "frameIndex":       random.randint(1, 1000),
        "transcript":       "The quadratic formula is x equals negative b plus or minus...",
        "speakerLabel":     "Teacher",
        "detectedText":     "x = -b ± √(b²-4ac) / 2a",
        "visualContext":    "Mathematical equation on whiteboard",
        "diagramDetected":  False,
        "mathDetected":     True,
        "chartDetected":    False,
        "whiteboardDetected": True,
        "currentAgent":     random.choice(["RETRIEVER", "CONTENT_GRADER", "PEDAGOGY"])
    }

# ── Health check ─────────────────────────────────────────────────
@app.get("/")
def root():
    return {"status": "ok", "app": "Edge Smartboard AI", "version": "1.0.0"}

if __name__ == "__main__":
    print("\n" + "="*55)
    print("  Edge Smartboard AI — Backend Server")
    print("="*55)
    print(f"  URL:  http://192.168.1.28:8000/")
    print(f"  Docs: http://192.168.1.28:8000/docs")
    print()
    print("  Login credentials:")
    print("    Email:    admin@edge.school")
    print("    Password: admin123")
    print()
    print("    Email:    admin@school.edu")
    print("    Password: admin123")
    print()
    print("    Email:    teacher@edge.school")
    print("    Password: teacher123")
    print("="*55 + "\n")

    uvicorn.run(app, host="0.0.0.0", port=8000, reload=False)
