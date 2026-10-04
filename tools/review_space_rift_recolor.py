"""Diagnostic only: show the game's exact recolor predicate on revised battle art."""
from pathlib import Path
from PIL import Image, ImageDraw
from qa_space_rift_battle_update import ART, QA, CROPS, STATES

canvas = Image.new('RGB', (1200, 4 * 310), '#16786f')
draw = ImageDraw.Draw(canvas)
for row, (form, crop) in enumerate(CROPS.items()):
    states = STATES if row < 2 else ('attack', 'double_attack')
    for col, state in enumerate(states):
        image = Image.open(ART / f'loem_{form}_battle_{state}_sheet.webp').convert('RGBA')
        frame = image.crop((0, 512, 512, 1024))  # F5, active pose
        tinted = []
        for r, g, b, a in frame.get_flattened_data():
            high, low = max(r, g, b), min(r, g, b)
            if a and 80 <= high <= 225 and high - low <= 30:
                tinted.append((int(70 * high / 190), min(255, int(220 * high / 190)), int(85 * high / 190), a))
            else:
                tinted.append((r, g, b, a))
        frame.putdata(tinted)
        x, y, w, h = crop
        frame = frame.crop((x, y, x+w, y+h))
        frame.thumbnail((196, 265))
        canvas.paste(frame, (col * 200, row * 310 + 35), frame)
        draw.text((col * 200 + 4, row * 310 + 4), form.removeprefix('space_rift_'), fill='white')
        draw.text((col * 200 + 4, row * 310 + 19), state, fill='white')
canvas.save(QA / 'recolor-review.jpg', quality=95)
