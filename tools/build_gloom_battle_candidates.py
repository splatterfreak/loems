"""Stage key-pose battle loops for visual QA; never publish automatically."""
from pathlib import Path
import json
import subprocess
import sys
from PIL import Image, ImageChops, ImageDraw
from generate_space_rift_battle_sheets import clear_transparent_rgb, prepare_canonical

ROOT = Path(__file__).resolve().parents[1]
ART = ROOT / 'art/intermediate'
OUT = ROOT / 'art/qa/gloom-battle'
STATES = ('attack', 'hit', 'double_attack', 'double_hit', 'victory', 'defeat')
OFFSETS = {
    'attack': ((0,0),(-6,0),(9,0),(12,0),(4,0),(0,0)),
    'hit': ((0,0),(-6,-3),(-11,-2),(-5,0),(-2,0),(0,0)),
    'double_attack': ((0,0),(10,0),(0,0),(12,0),(5,0),(0,0)),
    'double_hit': ((0,0),(-10,-4),(0,0),(-11,-3),(-3,0),(0,0)),
    'victory': ((0,0),(0,-3),(0,-6),(0,-8),(0,-4),(0,0)),
    'defeat': ((0,-10),(-2,-6),(2,-2),(-1,0),(0,0),(0,0)),
}

def place(pose, offset):
    cell = Image.new('RGBA', (512,512))
    cell.alpha_composite(pose, ((512-pose.width)//2+offset[0],448-pose.height+offset[1]))
    return clear_transparent_rgb(cell)

def recolor(im):
    out = im.copy()
    pixels = []
    for r,g,b,a in im.get_flattened_data():
        hi,lo = max(r,g,b),min(r,g,b)
        pixels.append((int(70*hi/190),min(255,int(220*hi/190)),int(85*hi/190),a)
                      if a and 80 <= hi <=225 and hi-lo <=30 else (r,g,b,a))
    out.putdata(pixels)
    return out

def main():
    OUT.mkdir(parents=True,exist_ok=True)
    logs, results, gallery = [], [], []
    tint_review = Image.new('RGB',(1200,800),'#24303a')
    tint_draw = ImageDraw.Draw(tint_review)
    for gender_index, gender in enumerate(('male','female')):
        idle = prepare_canonical(ART/f'gloom_{gender}_ref.png',(388,390))
        contact = Image.new('RGB',(6*280,6*290),'#24303a')
        draw = ImageDraw.Draw(contact)
        for row,state in enumerate(STATES):
            src = ART/f'loem_gloom_wizard_poop_{gender}_{state}_canonical.png'
            raw = Image.open(src)
            if raw.mode != 'RGBA' or raw.getchannel('A').getextrema() != (0,255):
                raise ValueError(f'Not genuine alpha: {src}')
            pose = prepare_canonical(src,(388,390))
            frames = [place(pose,off) for off in OFFSETS[state]]
            if state not in ('victory','defeat'):
                frames[0] = place(idle,(0,0))
                frames[5] = frames[0].copy()
                if state.startswith('double_'):
                    frames[2] = frames[0].copy()
            sheet = Image.new('RGBA',(1536,1024))
            crop_failures = []
            for i,frame in enumerate(frames):
                box = frame.getchannel('A').getbbox()
                if box[0]<48 or box[1]<48 or box[2]>464 or box[3]>464:
                    crop_failures.append((i+1,box))
                sheet.alpha_composite(frame,(i%3*512,i//3*512))
                preview = frame.crop((48,48,464,464))
                preview.thumbnail((265,265))
                contact.paste(preview,(i*280,row*290+23),preview)
                draw.text((i*280+4,row*290+3),f'{state} F{i+1}',fill='white')
            path = OUT/f'loem_gloom_wizard_poop_{gender}_battle_{state}_sheet.webp'
            sheet = clear_transparent_rgb(sheet)
            sheet.save(path,'WEBP',lossless=True,quality=100,method=6,exact=True)
            exact = ImageChops.difference(sheet,Image.open(path).convert('RGBA')).getbbox(alpha_only=False) is None
            cmd = [sys.executable,str(ROOT/'tools/qa_sprite_sheet.py'),str(path),
                   '--alpha-threshold','0','--maximum-center-drift','35',
                   '--maximum-ground-drift','14','--maximum-height-change','0.30',
                   '--minimum-loop-iou','0.40']
            result = subprocess.run(cmd,capture_output=True,text=True)
            logs.append(f'## {path.name}\n\nApp crop: (48,48,416,416).\n\n```text\n{result.stdout}\n```\nCrop failures: {crop_failures}\nLossless exact: {exact}\n')
            record = dict(file=path.name,qa_pass=result.returncode==0,crop_failures=crop_failures,
                          lossless_exact=exact,visual_approved=False)
            results.append(record)
            print(record,flush=True)
            gif = path.with_name(path.stem+'_preview.gif')
            gifcmd = [sys.executable,str(ROOT/'tools/sprite_sheet_to_gif.py'),str(path),str(gif),
                      '--columns','3','--rows','2','--duration','300','--background','24303A',
                      '--crop','48,48,416,416']
            if state in ('victory','defeat'):
                gifcmd.append('--once')
            subprocess.run(gifcmd,check=True,capture_output=True)
            gallery.append(f'<figure><figcaption>{gender} — {state}</figcaption><img src="{gif.name}"></figure>')
            tint = recolor(frames[3]).crop((48,48,464,464))
            tint.thumbnail((195,355))
            tint_review.paste(tint,(row*200,gender_index*400+30),tint)
            tint_draw.text((row*200+3,gender_index*400+4),f'{gender} {state}',fill='white')
        contact.save(OUT/f'{gender}_frames.jpg',quality=95)
    tint_review.save(OUT/'recolor-review.jpg',quality=95)
    (OUT/'results.json').write_text(json.dumps(results,indent=2),encoding='utf-8')
    (OUT/'frame-report.md').write_text('# Gloom battle candidate QA\n\nNumeric checks are not visual approval.\n\n'+'\n'.join(logs),encoding='utf-8')
    (OUT/'index.html').write_text('<!doctype html><meta charset="utf-8"><title>Zauberhaufen — Entwürfe</title>'
        '<style>body{background:#18232b;color:white;font:16px system-ui}main{display:flex;flex-wrap:wrap}figure{margin:10px;background:#24303a;padding:12px}img{width:300px}</style>'
        '<h1>Zauberhaufen — Animationsentwürfe</h1><p>Arbeitsstand. Freigabe siehe Prüfbericht. Sieg und Niederlage halten das letzte Bild.</p><main>'
        +''.join(gallery)+'</main>',encoding='utf-8')

if __name__=='__main__':
    main()
