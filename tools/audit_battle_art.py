"""Read-only sprite contact sheets and canonical reference extraction for art QA."""
from pathlib import Path
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'art/qa/projectiles'
OUT.mkdir(parents=True, exist_ok=True)
files = sorted((ROOT / 'app/src/main/res/drawable-nodpi').glob('*battle*sheet.webp'))
for start in range(0, len(files), 8):
    page = Image.new('RGB', (1200, 4 * 250), '#253442')
    draw = ImageDraw.Draw(page)
    for i, path in enumerate(files[start:start+8]):
        sheet = Image.open(path).convert('RGBA')
        # Each strip shows every frame, retaining differences and fired effects.
        columns = 4 if sheet.width == 2048 else 3
        cellw, cellh = sheet.width // columns, sheet.height // (3 if columns == 4 else 2)
        x, y = (i % 2)*600, (i // 2)*250
        draw.text((x+4, y+4), path.stem.replace('loem_', ''), fill='white')
        for k in range(columns * (3 if columns == 4 else 2)):
            cx, cy = k % columns * cellw, k // columns * cellh
            frame = sheet.crop((cx,cy,cx+cellw,cy+cellh))
            frame.thumbnail((98,205))
            page.paste(frame, (x+(k % 6)*100,y+25+(k//6)*105), frame)
    page.save(OUT / f'audit-{start//8:02}.jpg')
for form in ('ultra_cosmic', 'space_rift_archmage_poop'):
    sheet = Image.open(ROOT / f'app/src/main/res/drawable-nodpi/loem_{form}_idle_sheet.webp')
    sheet.crop((0,0,512,512)).save(OUT / f'{form}_idle.png')
print(f'{len(files)} battle sheets; {(len(files)+7)//8} contact pages in {OUT}')
