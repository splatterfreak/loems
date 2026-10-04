#!/usr/bin/env python3
"""Build stable young-Loem feeding sheets from two complete jaw key poses."""

from __future__ import annotations

from pathlib import Path

from PIL import Image, ImageChops, ImageDraw, ImageFilter

import standardize_all_ham_feeding as ham
import standardize_all_melon_feeding as melon


ROOT = Path(__file__).resolve().parents[1]
DRAWABLE = ROOT / "app" / "src" / "main" / "res" / "drawable-nodpi"
INTERMEDIATE = ROOT / "art" / "intermediate"
CELL = 512
FRAME_OFFSET = (-32, 0)
POSE_SEQUENCE = ("open", "closed", "open", "closed", "open", "closed")

# Complete connected mouth anatomy: rear corner, upper lip and snout, every
# tooth, oral cavity/tongue, full lower jaw/chin, and jaw-to-neck attachment.
FULL_JAW_POLYGON = (
    (270, 245),
    (480, 238),
    (495, 315),
    (470, 392),
    (350, 415),
    (286, 370),
    (266, 305),
)


def clean_transparency(image: Image.Image) -> Image.Image:
    rgba = image.convert("RGBA")
    red, green, blue, alpha = rgba.split()
    visible = alpha.point(lambda value: 255 if value else 0)
    empty = Image.new("L", rgba.size)
    return Image.merge(
        "RGBA",
        (
            Image.composite(red, empty, visible),
            Image.composite(green, empty, visible),
            Image.composite(blue, empty, visible),
            alpha,
        ),
    )


def articulation_mask() -> Image.Image:
    mask = Image.new("L", (CELL, CELL))
    ImageDraw.Draw(mask).polygon(FULL_JAW_POLYGON, fill=255)
    return mask.filter(ImageFilter.GaussianBlur(1.0))


def shifted(image: Image.Image) -> Image.Image:
    result = Image.new("RGBA", image.size)
    result.alpha_composite(image, FRAME_OFFSET)
    return result


def load_key_poses() -> tuple[Image.Image, Image.Image]:
    closed = clean_transparency(
        Image.open(INTERMEDIATE / "loem_feeding_closed_base_v3.png")
    )
    open_candidate = clean_transparency(
        Image.open(INTERMEDIATE / "loem_feeding_open_candidate_v3.png")
    )
    if closed.size != (CELL, CELL) or open_candidate.size != (CELL, CELL):
        raise ValueError("Young feeding key poses must both be 512x512")

    mask = articulation_mask()
    opened = clean_transparency(Image.composite(open_candidate, closed, mask))

    # The locked body may only differ inside the complete jaw mask plus its
    # antialiasing fringe. This prevents body/head jitter and partial mouths.
    allowed = mask.filter(ImageFilter.MaxFilter(7))
    outside = ImageChops.invert(allowed)
    outside_diff = ImageChops.multiply(
        ImageChops.difference(opened, closed).convert("RGB").convert("L"),
        outside,
    )
    if outside_diff.getbbox() is not None:
        raise ValueError("Open pose changes pixels outside the full-jaw region")

    difference = ImageChops.difference(opened, closed).convert("RGB").convert("L")
    bbox = difference.point(lambda value: 255 if value > 12 else 0).getbbox()
    if bbox is None:
        raise ValueError("Open and closed poses do not differ")
    left, top, right, bottom = bbox
    if right - left < 135 or bottom - top < 85 or right < 440 or bottom < 370:
        raise ValueError(f"Jaw articulation is incomplete: diff bbox={bbox}")
    print(f"Validated complete open/closed jaw articulation: diff bbox={bbox}")
    return opened, closed


def build_sheet(
    output: Path,
    food_config,
    props: dict[str, Image.Image],
    stages: list[str | None],
) -> None:
    opened, closed = load_key_poses()
    sheet = Image.new("RGBA", (CELL * 3, CELL * 2))
    for index, pose_name in enumerate(POSE_SEQUENCE):
        frame = (opened if pose_name == "open" else closed).copy()
        stage = stages[index]
        if stage is not None:
            prop = props[stage]
            frame.alpha_composite(
                prop,
                (food_config.prop_right - prop.width, food_config.prop_bottom - prop.height),
            )
        frame = clean_transparency(shifted(frame))
        row, column = divmod(index, 3)
        sheet.alpha_composite(frame, (column * CELL, row * CELL))
    clean_transparency(sheet).save(
        output,
        "WEBP",
        lossless=True,
        quality=100,
        method=6,
        exact=True,
    )


def main() -> None:
    ham_config = next(item for item in ham.CONFIGS if item.filename == "loem_feeding_sheet.webp")
    melon_config = next(item for item in melon.CONFIGS if item.filename == "loem_melon_sheet.webp")
    build_sheet(
        DRAWABLE / ham_config.filename,
        ham_config,
        ham.resized_props(ham_config.target_width),
        ham.stages(6),
    )
    build_sheet(
        DRAWABLE / melon_config.filename,
        melon_config,
        melon.resized_props(melon_config.target_width),
        melon.stages(6),
    )
    print("Built locked full-jaw young Loem ham and melon feeding sheets")


if __name__ == "__main__":
    main()
