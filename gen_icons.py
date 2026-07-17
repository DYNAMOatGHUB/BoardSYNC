"""
Generates all Android mipmap launcher icons from a source PNG.
Also produces a round version with a circular mask.
"""
from PIL import Image, ImageDraw
import os, shutil

SRC = r"C:\Users\PRANAV KARTHIK S\.gemini\antigravity\brain\7004b402-22c2-4de1-bbe2-5a12c797f91b\edge_app_icon_1784035867515.png"
BASE = r"C:\Users\PRANAV KARTHIK S\Desktop\app2\app\src\main\res"

SIZES = {
    "mipmap-mdpi":    48,
    "mipmap-hdpi":    72,
    "mipmap-xhdpi":   96,
    "mipmap-xxhdpi":  144,
    "mipmap-xxxhdpi": 192,
}

src = Image.open(SRC).convert("RGBA")

for folder, size in SIZES.items():
    out_dir = os.path.join(BASE, folder)
    os.makedirs(out_dir, exist_ok=True)

    # Standard icon
    icon = src.resize((size, size), Image.LANCZOS)
    icon.save(os.path.join(out_dir, "ic_launcher.png"))

    # Round icon – apply circular mask
    round_img = src.resize((size, size), Image.LANCZOS).copy()
    mask = Image.new("L", (size, size), 0)
    draw = ImageDraw.Draw(mask)
    draw.ellipse((0, 0, size, size), fill=255)
    round_img.putalpha(mask)
    round_img.save(os.path.join(out_dir, "ic_launcher_round.png"))

    print(f"  {folder}: {size}x{size}  OK")

print("\nAll icons generated!")
