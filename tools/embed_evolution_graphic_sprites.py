#!/usr/bin/env python3
"""Create compact base-form thumbnails, embed them, and validate the diagram."""

from __future__ import annotations

import argparse
import base64
import io
import re
from pathlib import Path

from PIL import Image


SPRITES = {
    "Junges Löm": "loem_idle_sheet.png",
    "Flügel-Löm": "loem_good_evolution_sheet.png",
    "Wurst-Löm": "loem_bad_evolution_sheet.png",
    "Majestätisches Flügel-Löm": "loem_wing_evolution_idle_sheet.png",
    "Matschkröten-Löm": "loem_mud_toad_idle_sheet.png",
    "Prunkschlangen-Löm": "loem_serpent_evolution_idle_sheet.png",
    "Haufen-Löm": "loem_poop_evolution_idle_sheet.png",
    "Sturmkaiser-Löm": "loem_stormkaiser_idle_sheet.png",
    "Sturmkaiserin-Löm": "loem_stormkaiser_female_idle_sheet.png",
    "Warzenkaiser-Löm": "loem_wart_emperor_male_idle_sheet.png",
    "Warzenkaiserin-Löm": "loem_wart_emperor_female_idle_sheet.png",
    "Armageddon-Prunkschlangenkaiser-Löm": "loem_armageddon_serpent_male_idle_sheet.png",
    "Armageddon-Prunkschlangenkaiserin-Löm": "loem_armageddon_serpent_female_idle_sheet.png",
    "Trübsal-Zauberhaufen-Löm": "loem_gloom_wizard_poop_male_idle_sheet.png",
    "Trübsal-Zauberhaufen-Lömin": "loem_gloom_wizard_poop_female_idle_sheet.png",
}


def make_thumbnail(sheet_path: Path) -> bytes:
    with Image.open(sheet_path) as source:
        sheet = source.convert("RGBA")
    frame_width, frame_height = sheet.width // 3, sheet.height // 2
    frame = sheet.crop((0, 0, frame_width, frame_height))
    bbox = frame.getchannel("A").point(lambda p: 255 if p > 24 else 0).getbbox()
    if bbox:
        frame = frame.crop(bbox)
    frame.thumbnail((120, 90), Image.Resampling.LANCZOS)
    canvas = Image.new("RGBA", (128, 96), (0, 0, 0, 0))
    canvas.alpha_composite(frame, ((128 - frame.width) // 2, (96 - frame.height) // 2))
    output = io.BytesIO()
    canvas.save(output, "WEBP", quality=82, method=6)
    return output.getvalue()


def replace_one(html: str, alt: str, data_uri: str) -> str:
    pattern = re.compile(
        r'(<img\s+src=")[^"]*("\s+alt="' + re.escape(alt) + r'"\s*>)'
    )
    updated, count = pattern.subn(lambda match: match.group(1) + data_uri + match.group(2), html)
    if count != 1:
        raise ValueError(f"Expected one image with alt={alt!r}, found {count}")
    return updated


def validate(html: str) -> None:
    matches = re.findall(
        r'<img\s+src="data:image/webp;base64,([^"]+)"\s+alt="([^"]+)"\s*>', html
    )
    if not matches:
        raise ValueError("No embedded evolution images found")
    found = {alt for _, alt in matches}
    missing = set(SPRITES) - found
    if missing:
        raise ValueError(f"Missing evolution images: {sorted(missing)}")
    unknown = found - set(SPRITES)
    if unknown:
        raise ValueError(f"Unknown evolution images: {sorted(unknown)}")
    for encoded, alt in matches:
        if len(encoded) >= 20_000:
            raise ValueError(f"Payload is unexpectedly large for {alt}: {len(encoded)}")
        raw = base64.b64decode(encoded, validate=True)
        with Image.open(io.BytesIO(raw)) as image:
            image.verify()


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("html", type=Path, help="Loems evolution HTML fragment to update")
    parser.add_argument(
        "--assets",
        type=Path,
        default=Path("app/src/main/res/drawable-nodpi"),
        help="Directory containing Loems sprite sheets",
    )
    args = parser.parse_args()

    html_path = args.html.resolve()
    asset_dir = args.assets.resolve()
    html = html_path.read_text(encoding="utf-8")
    present_alts = set(re.findall(r'<img\s+src="[^"]*"\s+alt="([^"]+)"\s*>', html))
    unknown = present_alts - set(SPRITES)
    if unknown:
        raise ValueError(f"Unknown evolution images: {sorted(unknown)}")
    for alt, filename in SPRITES.items():
        if alt not in present_alts:
            continue
        raw = make_thumbnail(asset_dir / filename)
        encoded = base64.b64encode(raw).decode("ascii")
        html = replace_one(html, alt, f"data:image/webp;base64,{encoded}")
    validate(html)
    html_path.write_text(html, encoding="utf-8")
    print(f"Embedded and validated {len(present_alts)} images in {html_path}")


if __name__ == "__main__":
    main()
