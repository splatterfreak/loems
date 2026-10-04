"""Stage the six dedicated Wurst battle states and their QA previews."""
from pathlib import Path
import json
import subprocess
import sys
from PIL import Image, ImageChops, ImageDraw
from build_gloom_battle_candidates import STATES, OFFSETS, place, recolor
from generate_space_rift_battle_sheets import clear_transparent_rgb, prepare_canonical

ROOT = Path(__file__).resolve().parents[1]
ART = ROOT / 'art/intermediate'
OUT = ROOT / 'art/qa/bad-battle'
CROP = (48,128,464,464)

def main():
    OUT.mkdir(parents=True,exist_ok=True)
    idle = prepare_canonical(ART/'bad_ref.png',(388,285))
    contact = Image.new('RGB',(1680,1500),'#24303a')
    draw = ImageDraw.Draw(contact)
    tint_contact = Image.new('RGB',(1200,300),'#24303a')
    records,logs,gallery = [],[],[]
    for row,state in enumerate(STATES):
        src=ART/f'loem_bad_{state}_canonical.png'
        raw=Image.open(src)
        if raw.mode!='RGBA' or raw.getchannel('A').getextrema()!=(0,255):
            raise ValueError(f'Invalid alpha: {src} {raw.mode} {raw.getextrema()}')
        maximum=(388,300) if state=='victory' else (388,285)
        pose=prepare_canonical(src,maximum)
        frames=[place(pose,o) for o in OFFSETS[state]]
        if state not in ('victory','defeat'):
            frames[0]=place(idle,(0,0))
            frames[5]=frames[0].copy()
            if state.startswith('double_'):
                frames[2]=frames[0].copy()
        sheet=Image.new('RGBA',(1536,1024))
        crop_failures=[]
        for i,frame in enumerate(frames):
            box=frame.getchannel('A').getbbox()
            if box[0]<CROP[0] or box[1]<CROP[1] or box[2]>CROP[2] or box[3]>CROP[3]:
                crop_failures.append((i+1,box))
            sheet.alpha_composite(frame,(i%3*512,i//3*512))
            prev=frame.crop(CROP)
            prev.thumbnail((270,220))
            contact.paste(prev,(i*280,row*250+25),prev)
            draw.text((i*280+4,row*250+3),f'{state} F{i+1}',fill='white')
        path=OUT/f'loem_bad_battle_{state}_sheet.webp'
        sheet=clear_transparent_rgb(sheet)
        sheet.save(path,'WEBP',lossless=True,quality=100,method=6,exact=True)
        exact=ImageChops.difference(sheet,Image.open(path).convert('RGBA')).getbbox(alpha_only=False) is None
        cmd=[sys.executable,str(ROOT/'tools/qa_sprite_sheet.py'),str(path),'--alpha-threshold','0',
             '--maximum-center-drift','35','--maximum-ground-drift','14',
             '--maximum-height-change','0.30','--minimum-loop-iou','0.40']
        result=subprocess.run(cmd,capture_output=True,text=True)
        logs.append(f'## {path.name}\n\nApp crop (48,128,416,336)\n\n```text\n{result.stdout}\n```\nCrop failures: {crop_failures}\nLossless exact: {exact}\n')
        record=dict(file=path.name,qa_pass=result.returncode==0,crop_failures=crop_failures,lossless_exact=exact)
        records.append(record)
        print(record,flush=True)
        gif=path.with_name(path.stem+'_preview.gif')
        cmd=[sys.executable,str(ROOT/'tools/sprite_sheet_to_gif.py'),str(path),str(gif),
             '--columns','3','--rows','2','--duration','300','--background','24303A','--crop','48,128,416,336']
        if state in ('victory','defeat'):
            cmd.append('--once')
        subprocess.run(cmd,check=True,capture_output=True)
        gallery.append(f'<figure><figcaption>{state}</figcaption><img src="{gif.name}"></figure>')
        tint=recolor(frames[3]).crop(CROP)
        tint.thumbnail((195,270))
        tint_contact.paste(tint,(row*200,20),tint)
    contact.save(OUT/'frames.jpg',quality=95)
    tint_contact.save(OUT/'recolor-review.jpg',quality=95)
    (OUT/'results.json').write_text(json.dumps(records,indent=2),encoding='utf-8')
    (OUT/'frame-report.md').write_text('# Wurst battle QA\n\n'+'\n'.join(logs),encoding='utf-8')
    (OUT/'index.html').write_text('<!doctype html><meta charset="utf-8"><title>Wurst — Kampfanimationen</title>'
       '<style>body{background:#18232b;color:white;font:16px system-ui}main{display:flex;flex-wrap:wrap}figure{background:#24303a;padding:12px}img{width:320px}</style>'
       '<h1>Wurst-Löm</h1><main>'+''.join(gallery)+'</main>',encoding='utf-8')

if __name__=='__main__':
    main()
