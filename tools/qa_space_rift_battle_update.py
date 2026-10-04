"""Reproducible QA and rendered previews for the projectile-free battle update."""
from pathlib import Path
import subprocess
import sys
import json
import shutil
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
ART = ROOT / 'app/src/main/res/drawable-nodpi'
QA = ROOT / 'art/qa/projectiles'
STATES = ('attack', 'hit', 'double_attack', 'double_hit', 'victory', 'defeat')
CROPS = {
    'ultra_cosmic': (48, 96, 416, 368),
    'space_rift_archmage_poop': (48, 48, 416, 416),
    'space_rift_urtoad': (48, 176, 416, 288),
    'space_rift_world_serpent': (48, 160, 416, 304),
}


def recolor_body(image: Image.Image, color: tuple[int, int, int]) -> Image.Image:
    """Matches the Android neutral-gray mask for visual recolor QA."""
    result = image.convert('RGBA')
    pixels = result.load()
    for y in range(result.height):
        for x in range(result.width):
            red, green, blue, alpha = pixels[x, y]
            maximum, minimum = max(red, green, blue), min(red, green, blue)
            if alpha and 80 <= maximum <= 225 and maximum - minimum <= 30:
                shade = maximum / 190
                pixels[x, y] = (*[round(component * shade) for component in color], alpha)
    return result


def main():
    QA.mkdir(parents=True, exist_ok=True)
    records = []
    logs = []
    gallery = []
    for form, crop in CROPS.items():
        states = STATES
        contact = Image.new('RGB', (6 * 300, len(states) * 310), '#24303a')
        draw = ImageDraw.Draw(contact)
        for row, state in enumerate(states):
            path = ART / f'loem_{form}_battle_{state}_sheet.webp'
            if not path.exists():
                raise FileNotFoundError(path)
            image = Image.open(path).convert('RGBA')
            frames = []
            crop_failures = []
            x, y, w, h = crop
            for i in range(12):
                cx, cy = i % 4 * 512, i // 4 * 512
                frame = image.crop((cx, cy, cx + 512, cy + 512))
                bounds = frame.getchannel('A').getbbox()
                if bounds[0] < x or bounds[1] < y or bounds[2] > x + w or bounds[3] > y + h:
                    crop_failures.append((i + 1, bounds))
                frames.append(frame)
            cmd = [sys.executable, str(ROOT / 'tools/qa_sprite_sheet.py'), str(path),
                   '--columns', '4', '--rows', '3', '--alpha-threshold', '0',
                   '--maximum-center-drift', '35', '--maximum-ground-drift', '18',
                   '--maximum-height-change', '0.30', '--minimum-loop-iou', '0.35']
            result = subprocess.run(cmd, capture_output=True, text=True)
            logs.append(f'\n## {path.name}\nApp crop: {crop}\n{result.stdout}\nCrop failures: {crop_failures}\n')
            record = {'file': path.name, 'crop': crop, 'qa_pass': result.returncode == 0,
                      'crop_failures': crop_failures}
            records.append(record)
            for col, i in enumerate((0, 2, 3, 5, 8, 11)):
                preview = frames[i].crop((x, y, x + w, y + h))
                preview.thumbnail((288, 280))
                px, py = col * 300 + (300 - preview.width) // 2, row * 310 + 24
                contact.paste(preview, (px, py), preview)
                draw.text((col * 300 + 8, row * 310 + 4), f'{state} F{i + 1}', fill='white')
            if form == 'space_rift_urtoad' and state in ('attack', 'hit', 'defeat'):
                recolored = Image.new('RGB', (6 * 300, 310), '#24303a')
                recolor_draw = ImageDraw.Draw(recolored)
                for col, i in enumerate((0, 2, 3, 5, 8, 11)):
                    preview = recolor_body(frames[i], (69, 139, 222)).crop((x, y, x + w, y + h))
                    preview.thumbnail((288, 280))
                    px, py = col * 300 + (300 - preview.width) // 2, 24
                    recolored.paste(preview, (px, py), preview)
                    recolor_draw.text((col * 300 + 8, 4), f'{state} F{i + 1}', fill='white')
                recolored.save(QA / f'{path.stem}_recolor_review.jpg', quality=95)
            gif = QA / f'{path.stem}_preview.gif'
            gif_cmd = [sys.executable, str(ROOT / 'tools/sprite_sheet_to_gif.py'), str(path), str(gif),
                       '--columns', '4', '--rows', '3', '--duration', '100',
                       '--background', '24303A', '--crop', ','.join(map(str, crop))]
            if state in ('victory', 'defeat'):
                gif_cmd.append('--once')
            subprocess.run(gif_cmd, check=True)
            # Keep reviewed WebP/GIF pairs outside Android resources.
            shutil.copy2(path, QA / path.name)
            shutil.copy2(gif, ROOT / 'art/previews' / f'{path.stem.removesuffix("_sheet")}_preview.gif')
            gallery.append(f'<figure><figcaption>{form} — {state}</figcaption>'
                           f'<img src="{gif.name}" alt="{form} {state}">'
                           f'<a href="{path.name}">Lossless WebP</a></figure>')
            print(record, flush=True)
        contact.save(QA / f'{form}_battle_review.jpg', quality=95)
    (QA / 'frame-report.md').write_text('# Battle sprite QA\n' + '\n'.join(logs), encoding='utf-8')
    (QA / 'results.json').write_text(json.dumps(records, indent=2), encoding='utf-8')
    (QA / 'index.html').write_text(
        '<!doctype html><html lang="de"><meta charset="utf-8"><title>Raumriss — Kampfanimationen</title>'
        '<style>body{background:#18232b;color:#f4f4f4;font:16px system-ui;margin:24px}'
        'main{display:grid;grid-template-columns:repeat(auto-fit,minmax(330px,1fr));gap:20px}'
        'figure{margin:0;padding:16px;background:#24303a;border-radius:12px}'
        'img{display:block;width:100%;max-width:416px;height:360px;object-fit:contain}'
        'a{color:#ade0ff}figcaption{min-height:42px}</style>'
        '<h1>Raumriss — geprüfte Kampfanimationen</h1>'
        '<p>Angriff, Treffer, Doppelangriff, Doppeltreffer, Sieg und Niederlage. '
        'Jede Raumriss-Form wird in allen Kampfzuständen geprüft. '
        'Keine eigenen Geschosse. Sieg/Niederlage spielen einmal; zum Wiederholen Seite neu laden.</p>'
        '<main>' + ''.join(gallery) + '</main></html>', encoding='utf-8')
    return int(any(not r['qa_pass'] or r['crop_failures'] for r in records))


if __name__ == '__main__':
    sys.exit(main())
