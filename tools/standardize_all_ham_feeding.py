#!/usr/bin/env python3
"""Apply the approved fixed-bone ham progression to every Loems feeding sheet."""

from __future__ import annotations

import shutil
from dataclasses import dataclass
from pathlib import Path

from PIL import Image, ImageChops, ImageDraw, ImageFilter


ROOT = Path(__file__).resolve().parents[1]
DRAWABLE = ROOT / "app" / "src" / "main" / "res" / "drawable-nodpi"
PREVIEWS = ROOT / "art" / "previews"
BACKUPS = ROOT / "art" / "intermediate" / "ham_sheets_before_fixed_bone_standard"
CELL = 512


@dataclass(frozen=True)
class HamConfig:
    filename: str
    anchor_right: int
    anchor_bottom: int
    target_width: int = 105
    contact_right: int | None = None
    contact_bottom: int | None = None

    @property
    def prop_right(self) -> int:
        return self.contact_right if self.contact_right is not None else self.anchor_right

    @property
    def prop_bottom(self) -> int:
        return self.contact_bottom if self.contact_bottom is not None else self.anchor_bottom


CONFIGS = (
    HamConfig("loem_feeding_sheet.webp", 462, 343, contact_right=455, contact_bottom=306),
    HamConfig("loem_armageddon_serpent_female_ham_sheet.webp", 461, 339),
    HamConfig("loem_armageddon_serpent_male_ham_sheet.webp", 463, 339),
    HamConfig("loem_bad_ham_sheet.webp", 479, 415, contact_right=481, contact_bottom=281),
    HamConfig("loem_gloom_wizard_poop_female_ham_sheet.webp", 442, 300, 92, 422, 367),
    HamConfig("loem_gloom_wizard_poop_male_ham_sheet.webp", 453, 325, 92, 422, 367),
    HamConfig("loem_mud_toad_ham_sheet.webp", 461, 370, 96, 466, 333),
    HamConfig("loem_poop_ham_sheet.webp", 466, 422, contact_right=475, contact_bottom=354),
    HamConfig("loem_serpent_ham_sheet.webp", 464, 372, 92, 467, 297),
    HamConfig("loem_space_rift_archmage_poop_ham_sheet.webp", 452, 320, 78),
    HamConfig("loem_space_rift_urtoad_ham_sheet.webp", 452, 373, 82),
    HamConfig("loem_space_rift_world_serpent_ham_sheet.webp", 431, 330, 82),
    HamConfig("loem_stormkaiser_female_ham_sheet.webp", 435, 396, 100, 475, 340),
    HamConfig("loem_stormkaiser_ham_sheet.webp", 435, 451, 100, 475, 340),
    HamConfig("loem_ultra_cosmic_ham_sheet.webp", 407, 455, 72, 447, 293),
    HamConfig("loem_wart_emperor_female_ham_sheet.webp", 455, 374, 98),
    HamConfig("loem_wart_emperor_male_ham_sheet.webp", 463, 374, 98),
    HamConfig("loem_wing_evolution_ham_sheet.webp", 453, 457, 96, 476, 306),
)


def frame(sheet: Image.Image, index: int, columns: int) -> Image.Image:
    return sheet.crop(
        (
            (index % columns) * CELL,
            (index // columns) * CELL,
            (index % columns + 1) * CELL,
            (index // columns + 1) * CELL,
        )
    )


def resized_props(target_width: int) -> list[Image.Image]:
    source = [
        Image.open(PREVIEWS / f"loem_good_ham_bite_{index}.webp").convert("RGBA")
        for index in range(5)
    ]
    scale = target_width / source[0].width
    return [
        prop.resize(
            (max(1, round(prop.width * scale)), max(1, round(prop.height * scale))),
            Image.Resampling.LANCZOS,
        )
        for prop in source
    ]


def prop_cleanup_mask(
    current: Image.Image,
    anchor_right: int,
    anchor_bottom: int,
    target_width: int,
) -> Image.Image:
    """Select old red meat, cream bone, and their outline inside the prop region."""
    left = max(0, anchor_right - target_width - 180)
    top = max(0, anchor_bottom - round(target_width * 1.50) - 80)
    right = min(CELL, anchor_right + 64)
    bottom = min(CELL, anchor_bottom + 90)
    source = current.load()
    mask = Image.new("L", current.size)
    target = mask.load()
    for y in range(top, bottom):
        for x in range(left, right):
            red, green, blue, alpha = source[x, y]
            # Include dark red meat, orange highlights, pink cut faces, and
            # detached warm crumbs from the previous generated prop.
            meat = (
                red >= 72
                and red >= green * 0.88
                and red >= blue * 1.12
                and max(red, green, blue) - min(red, green, blue) >= 30
            )
            bone = red >= 145 and green >= 100 and blue >= 58 and green >= blue * 0.88
            if alpha > 20 and (meat or bone):
                target[x, y] = 255
    mask = mask.filter(ImageFilter.MaxFilter(17)).filter(ImageFilter.GaussianBlur(0.65))
    # Never let the cleanup grow outside the intended food interaction region.
    bounds = Image.new("L", current.size)
    ImageDraw.Draw(bounds).rectangle((left, top, right - 1, bottom - 1), fill=255)
    return Image.composite(mask, Image.new("L", current.size), bounds)


def complete_prop_region(anchor_right: int, anchor_bottom: int, target_width: int) -> Image.Image:
    """Cover the complete legacy prop/holding area, including brown outlines.

    Several legacy final frames still contain food, so color segmentation alone
    cannot reliably erase every crust, rind or outline pixel. The matching
    hungry sheet supplies the clean anatomy for this bounded interaction area.
    """
    left = max(0, anchor_right - target_width - 95)
    top = max(0, anchor_bottom - round(target_width * 2.55) - 35)
    right = min(CELL, anchor_right + 35)
    bottom = min(CELL, anchor_bottom + 35)
    mask = Image.new("L", (CELL, CELL))
    ImageDraw.Draw(mask).rounded_rectangle((left, top, right, bottom), radius=24, fill=255)
    return mask.filter(ImageFilter.GaussianBlur(1.2))


def stages(frame_count: int) -> tuple[int | None, ...]:
    if frame_count == 6:
        return (0, 1, 2, 3, 4, None)
    if frame_count == 12:
        return (0, 0, 1, 1, 2, 2, 3, 3, 4, 4, None, None)
    raise ValueError(f"Unsupported feeding frame count: {frame_count}")


def standardize(config: HamConfig) -> None:
    path = DRAWABLE / config.filename
    BACKUPS.mkdir(parents=True, exist_ok=True)
    backup = BACKUPS / config.filename
    if not backup.exists():
        shutil.copy2(path, backup)
    # Always rebuild from the untouched pre-standardization sheet. This makes
    # the operation idempotent and prevents old/new food layers from stacking.
    source = Image.open(backup).convert("RGBA")
    if source.size == (CELL * 3, CELL * 2):
        columns, rows = 3, 2
    elif source.size == (CELL * 4, CELL * 3):
        columns, rows = 4, 3
    else:
        raise ValueError(f"Unsupported sheet geometry: {path} {source.size}")

    frame_count = columns * rows
    hungry_name = (
        "loem_hungry_sheet.webp"
        if config.filename == "loem_feeding_sheet.webp"
        else config.filename.replace("_ham_sheet.webp", "_hungry_sheet.webp")
    )
    hungry = Image.open(DRAWABLE / hungry_name).convert("RGBA")
    hungry_columns = 4 if hungry.size == (CELL * 4, CELL * 3) else 3
    final_clean = (
        frame(source, frame_count - 1, columns)
        if config.filename == "loem_bad_ham_sheet.webp"
        else frame(hungry, 0, hungry_columns)
    )
    props = resized_props(config.target_width)
    result = Image.new("RGBA", source.size)
    for index, stage in enumerate(stages(frame_count)):
        current = frame(source, index, columns)
        cleanup = prop_cleanup_mask(
            current,
            config.anchor_right,
            config.anchor_bottom,
            config.target_width,
        )
        if config.filename != "loem_bad_ham_sheet.webp":
            cleanup = ImageChops.lighter(
                cleanup,
                complete_prop_region(config.anchor_right, config.anchor_bottom, config.target_width),
            )
        current = Image.composite(final_clean, current, cleanup)
        if stage is not None:
            prop = props[stage]
            current.alpha_composite(
                prop,
                (
                    config.prop_right - prop.width,
                    config.prop_bottom - prop.height,
                ),
            )
        result.alpha_composite(current, ((index % columns) * CELL, (index // columns) * CELL))

    result.save(path, "WEBP", lossless=True, quality=100, method=6)
    print(f"standardized {config.filename}: {columns}x{rows}, width={config.target_width}")


def main() -> None:
    for config in CONFIGS:
        standardize(config)


if __name__ == "__main__":
    main()
