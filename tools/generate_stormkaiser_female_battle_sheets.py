from pathlib import Path
from PIL import Image, ImageChops

CELL=512; DRAWABLE=Path('app/src/main/res/drawable-nodpi'); INTERMEDIATE=Path('art/intermediate')
STATES=('attack','hit','double_attack','double_hit','victory','defeat')
def clean(im):
    im=im.convert('RGBA'); im.putdata([(0,0,0,0) if a==0 else (r,g,b,a) for r,g,b,a in im.getdata()]); return im
def pose(path, max_size):
    im=Image.open(path).convert('RGBA'); a=im.getchannel('A').point(lambda v:0 if v<=16 else v); im.putalpha(a); box=a.getbbox()
    if not box: raise ValueError(path)
    im=im.crop(box); scale=min(max_size[0]/im.width,max_size[1]/im.height)
    return clean(im.resize((round(im.width*scale),round(im.height*scale)),Image.Resampling.LANCZOS))
def frame(p,off=(0,0),ground=447):
    out=Image.new('RGBA',(CELL,CELL)); out.alpha_composite(p,((CELL-p.width)//2+off[0],ground-p.height+1+off[1])); return clean(out)
def write(frames,path):
    sheet=Image.new('RGBA',(CELL*3,CELL*2))
    for i,f in enumerate(frames): sheet.alpha_composite(f,(i%3*CELL,i//3*CELL))
    sheet=clean(sheet); path.parent.mkdir(parents=True,exist_ok=True); sheet.save(path,'WEBP',lossless=True,quality=100,method=6,exact=True)
    if ImageChops.difference(sheet,Image.open(path).convert('RGBA')).getbbox(alpha_only=False): raise ValueError(path)
idle=pose(INTERMEDIATE/'stormkaiser_female_idle_ref.png',(380,330))
offsets={'attack':((0,0),(-8,0),(10,0),(14,0),(5,0),(0,0)),'hit':((0,0),(-6,-4),(-12,-3),(-6,0),(-2,0),(0,0)),'double_attack':((0,0),(8,0),(16,0),(-4,0),(10,0),(0,0)),'double_hit':((0,0),(-7,-5),(-13,-3),(-5,0),(-2,0),(0,0)),'victory':((0,0),(0,-5),(0,-10),(0,-14),(0,-7),(0,0)),'defeat':((0,-12),(-2,-6),(2,0),(-1,-2),(0,0),(0,0))}
for state in STATES:
    max_size=(380,330) if state not in ('defeat',) else (390,245)
    p=pose(INTERMEDIATE/f'loem_stormkaiser_female_{state}_canonical.png',max_size)
    fs=[frame(p,o) for o in offsets[state]] if state in ('victory','defeat') else [frame(idle,offsets[state][0]),* [frame(p,o) for o in offsets[state][1:5]],frame(idle,offsets[state][5])]
    write(fs,DRAWABLE/f'loem_stormkaiser_female_battle_{state}_sheet.webp')
