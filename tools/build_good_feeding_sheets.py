#!/usr/bin/env python3
"""Rebuild Good-form feeds with fixed-size props and cumulative bite contours."""

from __future__ import annotations

import argparse
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter


CELL = 512
CLOSED = (False, True, False, True, False, True)
BITE_STAGES: tuple[int | None, ...] = (0, 1, 2, 3, 4, None)
ROOT = Path(__file__).resolve().parents[1]
CLOSED_CANONICAL = ROOT / "art" / "previews" / "loem_good_closed_chew_canonical.webp"
OPEN_CANONICAL = ROOT / "art" / "previews" / "loem_good_open_canonical.webp"
MELON_BITE_PREFIX = ROOT / "art" / "previews" / "loem_good_melon_bite"
HAM_BITE_PREFIX = ROOT / "art" / "previews" / "loem_good_ham_bite"
GENERATED_CLOSED_JAW = ROOT / "art" / "intermediate" / "loem_good_closed_jaw_generated.png"


def clean_transparency(image: Image.Image) -> Image.Image:
    """Remove hidden RGB data so transparent regions cannot render as rectangular ghosts."""
    rgba = image.convert("RGBA")
    rgba.putdata(
        [
            (red, green, blue, alpha) if alpha else (0, 0, 0, 0)
            for red, green, blue, alpha in rgba.getdata()
        ],
    )
    return rgba


def register_closed_jaw(open_pose: Image.Image, closed_pose: Image.Image) -> Image.Image:
    """Keep the approved body fixed while replacing the complete articulated jaw anatomy."""
    opened = clean_transparency(open_pose)
    if GENERATED_CLOSED_JAW.exists():
        generated_crop = clean_transparency(
            Image.open(GENERATED_CLOSED_JAW).convert("RGBA"),
        ).resize((256, 256), Image.Resampling.LANCZOS)
        original_crop = opened.crop((220, 120, 476, 376))
        jaw_mask = Image.new("L", original_crop.size)
        ImageDraw.Draw(jaw_mask).ellipse((42, 38, 224, 174), fill=255)
        jaw_mask = jaw_mask.filter(ImageFilter.GaussianBlur(1.0))
        repaired_crop = Image.composite(generated_crop, original_crop, jaw_mask)
        repaired = opened.copy()
        repaired.paste(clean_transparency(repaired_crop), (220, 120))
        return clean_transparency(repaired)

    aligned_closed = Image.new("RGBA", opened.size)
    # The old closed drawing is 5 px left and 6 px lower than the open canonical.
    # Register the eye/head landmarks before taking its mouth and cheek anatomy.
    aligned_closed.alpha_composite(clean_transparency(closed_pose), (5, -6))

    jaw_mask = Image.new("L", opened.size)
    ImageDraw.Draw(jaw_mask).ellipse((258, 150, 432, 302), fill=255)
    jaw_mask = jaw_mask.filter(ImageFilter.GaussianBlur(1.1))
    return clean_transparency(Image.composite(aligned_closed, opened, jaw_mask))


def frame(sheet: Image.Image, index: int) -> Image.Image:
    return sheet.crop(((index % 3) * CELL, (index // 3) * CELL, (index % 3 + 1) * CELL, (index // 3 + 1) * CELL))


def extract_food(source: Image.Image, box: tuple[int, int, int, int], food: str) -> Image.Image:
    left, top, right, bottom = box
    candidates: set[tuple[int, int]] = set()
    pixels = source.load()
    for y in range(top, bottom):
        for x in range(left, right):
            red, green, blue, alpha = pixels[x, y]
            warm = red >= 100 and red >= green * 1.08 and red >= blue * 1.15
            rind = food == "melon" and green >= 65 and green >= red * 0.48 and green >= blue * 1.12
            if alpha > 24 and max(red, green, blue) - min(red, green, blue) >= 32 and (warm or rind):
                candidates.add((x, y))

    components: list[set[tuple[int, int]]] = []
    while candidates:
        pending = [candidates.pop()]
        component = {pending[0]}
        while pending:
            x, y = pending.pop()
            for neighbor in ((x - 1, y), (x + 1, y), (x, y - 1), (x, y + 1)):
                if neighbor in candidates:
                    candidates.remove(neighbor)
                    component.add(neighbor)
                    pending.append(neighbor)
        components.append(component)
    component = max(components, key=len)
    mask = Image.new("L", source.size)
    target = mask.load()
    for x, y in component:
        target[x, y] = 255
    mask = mask.filter(ImageFilter.MaxFilter(11)).filter(ImageFilter.GaussianBlur(0.6))
    bbox = mask.getbbox()
    if bbox is None:
        raise ValueError(f"Could not isolate {food}")
    layer = Image.new("RGBA", source.size)
    layer.paste(source, mask=mask)
    return layer.crop(bbox)


def build(
    open_pose: Image.Image,
    closed_pose: Image.Image,
    props: list[Image.Image],
    anchor_right: int,
    anchor_bottom: int,
    output: Path,
) -> None:
    result = Image.new("RGBA", (CELL * 3, CELL * 2))
    for index, stage in enumerate(BITE_STAGES):
        # Each cell starts as one complete, mutually exclusive full-body pose.
        # Never retain a previous head/eye and never layer a rectangular mouth patch.
        current = clean_transparency(closed_pose if CLOSED[index] else open_pose)
        if stage is not None:
            prop = clean_transparency(props[stage])
            current.alpha_composite(prop, (anchor_right - prop.width, anchor_bottom - prop.height))
        result.alpha_composite(current, ((index % 3) * CELL, (index // 3) * CELL))
    output.parent.mkdir(parents=True, exist_ok=True)
    clean_transparency(result).save(
        output,
        "WEBP",
        lossless=True,
        quality=100,
        method=6,
        exact=True,
    )


def write_jaw_preview(sheet_path: Path, output_path: Path) -> None:
    sheet = Image.open(sheet_path).convert("RGBA")
    crop_box = (250, 145, 490, 365)
    scale = 2
    label_height = 28
    frames: list[Image.Image] = []
    for index in range(6):
        current = frame(sheet, index).crop(crop_box)
        backdrop = Image.new("RGBA", current.size, (244, 240, 247, 255))
        backdrop.alpha_composite(current)
        frames.append(
            backdrop.resize(
                (current.width * scale, current.height * scale),
                Image.Resampling.NEAREST,
            ),
        )

    output = Image.new(
        "RGB",
        (frames[0].width * 3, (frames[0].height + label_height) * 2),
        "white",
    )
    draw = ImageDraw.Draw(output)
    for index, current in enumerate(frames):
        x = (index % 3) * current.width
        y = (index // 3) * (current.height + label_height)
        output.paste(current.convert("RGB"), (x, y))
        draw.text((x + 8, y + current.height + 5), f"Frame {index + 1}", fill="black")
    output_path.parent.mkdir(parents=True, exist_ok=True)
    output.save(output_path, "PNG")


def write_jaw_edit_target(open_pose: Image.Image, output_path: Path) -> None:
    crop = clean_transparency(open_pose).crop((220, 120, 476, 376))
    keyed = Image.new("RGBA", crop.size, (0, 255, 0, 255))
    keyed.alpha_composite(crop)
    keyed = keyed.resize((1024, 1024), Image.Resampling.LANCZOS)
    output_path.parent.mkdir(parents=True, exist_ok=True)
    keyed.convert("RGB").save(output_path, "PNG")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("melon_sheet", type=Path)
    parser.add_argument("ham_sheet", type=Path)
    parser.add_argument("--jaw-preview-dir", type=Path)
    args = parser.parse_args()

    open_pose = Image.open(OPEN_CANONICAL).convert("RGBA")
    closed_pose = register_closed_jaw(
        open_pose,
        Image.open(CLOSED_CANONICAL).convert("RGBA"),
    )
    melon_props = [Image.open(f"{MELON_BITE_PREFIX}_{index}.webp").convert("RGBA") for index in range(5)]
    ham_props = [Image.open(f"{HAM_BITE_PREFIX}_{index}.webp").convert("RGBA") for index in range(5)]
    build(open_pose, closed_pose, melon_props, 480, 408, args.melon_sheet)
    build(open_pose, closed_pose, ham_props, 440, 346, args.ham_sheet)
    if args.jaw_preview_dir:
        write_jaw_edit_target(
            open_pose,
            args.jaw_preview_dir / "loem_good_open_jaw_edit_target.png",
        )
        write_jaw_preview(
            args.melon_sheet,
            args.jaw_preview_dir / "loem_good_melon_jaw_contact.png",
        )
        write_jaw_preview(
            args.ham_sheet,
            args.jaw_preview_dir / "loem_good_ham_jaw_contact.png",
        )


if __name__ == "__main__":
    main()
