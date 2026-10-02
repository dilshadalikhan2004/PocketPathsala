# LENTERA 2.0 — CAMERA & VISION PIPELINE

## Flow Architecture
```
CameraX Live Preview
       ↓
  Image Capture (High resolution)
       ↓
  Preprocessing (Edge detection, Perspective correction, Contrast normalization)
       ↓
  Local OCR (ML Kit On-Device Text Recognition)
       ↓
  Layout Detection & Problem Extraction
       ↓
  Topic Classification & Curriculum Mapping
       ↓
  Step-by-Step AI Problem Solver / Tutor
```

## Problem Solving Socratic Mode
When a math or physics problem is detected, LENTERA avoids revealing the final answer immediately. It defaults to Socratic guidance:
1. Identify problem statement.
2. Explain key concepts & formula.
3. Prompt student for the first step.
4. Evaluate student response.
5. Provide final step-by-step solution when requested.
