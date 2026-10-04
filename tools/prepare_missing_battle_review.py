"""Extract legacy reference and inspect generated key poses without changing art."""
from pathlib import Path
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
ART = ROOT / 'art/intermediate'
RES = ROOT / 'app/src/main/res/drawable-nodpi'

def main():
    source = Image.open(RES / 'loem_bad_evolution_sheet.webp').convert('RGBA')
    ref = source.crop((134, 181, 496, 437))
    cell = Image.new('RGBA', (512, 512))
    cell.alpha_composite(ref, (75, 191))
    cell.save(ART / 'bad_ref.png')
    files = sorted(ART.glob('loem_gloom_wizard_poop_*_canonical.png'))
    contact = Image.new('RGB', (1200, 400 * ((len(files)+3)//4)), '#24303a')
    draw = ImageDraw.Draw(contact)
    for i, path in enumerate(files):
        im = Image.open(path)
        print(path.name, im.mode, im.getextrema()[-1], im.size)
        im = im.convert('RGBA')
        im.thumbnail((290, 350))
        x, y = i % 4 * 300, i // 4 * 400
        contact.paste(im, (x, y+30), im)
        draw.text((x+3,y+3), path.stem.replace('loem_gloom_wizard_poop_', ''), fill='white')
    contact.save(ART / 'gloom_canonical_review.jpg')

if __name__ == '__main__':
    main()
